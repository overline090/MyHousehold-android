package org.myhousehold.app.reseau

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import java.net.Inet4Address
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.myhousehold.core.AdresseServeur

/** Le type annoncé par le serveur sur le réseau local. */
private const val TYPE_DE_SERVICE = "_myhousehold._tcp."

data class ServeurTrouve(val nom: String, val adresse: AdresseServeur)

sealed interface EtatDetection {
    /** Un serveur de maison ne s'annonce pas sur le réseau mobile. */
    data object HorsWifi : EtatDetection
    data object Recherche : EtatDetection
    data class Trouves(val serveurs: List<ServeurTrouve>) : EtatDetection
}

/**
 * Cherche les serveurs MyHousehold annoncés sur le Wi-Fi (mDNS), tant
 * qu'on l'écoute — et seulement en Wi-Fi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun detecterLesServeurs(contexte: Context): Flow<EtatDetection> =
    enWifi(contexte).flatMapLatest { wifi ->
        if (wifi) annonces(contexte) else flowOf(EtatDetection.HorsWifi)
    }

private fun enWifi(contexte: Context): Flow<Boolean> = callbackFlow {
    val connectivite = contexte.getSystemService(ConnectivityManager::class.java)
    val reseaux = mutableSetOf<Network>()
    val rappel = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(reseau: Network) {
            reseaux += reseau
            trySend(true)
        }

        override fun onLost(reseau: Network) {
            reseaux -= reseau
            trySend(reseaux.isNotEmpty())
        }
    }
    trySend(false)
    connectivite.registerNetworkCallback(
        NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
        rappel,
    )
    awaitClose { connectivite.unregisterNetworkCallback(rappel) }
}.distinctUntilChanged()

private fun annonces(contexte: Context): Flow<EtatDetection> = callbackFlow {
    val nsd = contexte.getSystemService(NsdManager::class.java)
    val wifi = contexte.applicationContext.getSystemService(WifiManager::class.java)
    // Sans ce verrou, beaucoup de téléphones jettent le multicast pour
    // économiser la batterie, et rien n'est jamais trouvé.
    val verrou = wifi.createMulticastLock("myhousehold-detection").apply {
        setReferenceCounted(false)
        acquire()
    }

    val trouves = linkedMapOf<String, ServeurTrouve>()
    // Avant Android 14, une seule résolution à la fois : les annonces
    // attendent leur tour.
    val aResoudre = Channel<NsdServiceInfo>(Channel.UNLIMITED)
    send(EtatDetection.Recherche)

    launch {
        for (service in aResoudre) {
            val resolu = resoudre(nsd, service) ?: continue
            val ip = resolu.host as? Inet4Address ?: continue
            val serveur = ServeurTrouve(
                nom = resolu.serviceName,
                adresse = AdresseServeur("https://${ip.hostAddress}:${resolu.port}"),
            )
            // Les annonces perdues arrivent sur un autre fil.
            val liste = synchronized(trouves) {
                trouves[resolu.serviceName] = serveur
                trouves.values.toList()
            }
            send(EtatDetection.Trouves(liste))
        }
    }

    val ecoute = object : NsdManager.DiscoveryListener {
        override fun onServiceFound(service: NsdServiceInfo) {
            aResoudre.trySend(service)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            val reste = synchronized(trouves) {
                trouves.remove(service.serviceName) ?: return
                trouves.values.toList()
            }
            trySend(if (reste.isEmpty()) EtatDetection.Recherche else EtatDetection.Trouves(reste))
        }

        override fun onDiscoveryStarted(type: String) = Unit
        override fun onDiscoveryStopped(type: String) = Unit
        override fun onStartDiscoveryFailed(type: String, code: Int) = Unit
        override fun onStopDiscoveryFailed(type: String, code: Int) = Unit
    }
    nsd.discoverServices(TYPE_DE_SERVICE, NsdManager.PROTOCOL_DNS_SD, ecoute)

    awaitClose {
        runCatching { nsd.stopServiceDiscovery(ecoute) }
        aResoudre.close()
        verrou.release()
    }
}

@Suppress("DEPRECATION") // registerServiceInfoCallback n'existe qu'à partir d'Android 14.
private suspend fun resoudre(nsd: NsdManager, service: NsdServiceInfo): NsdServiceInfo? =
    suspendCancellableCoroutine { suite ->
        nsd.resolveService(
            service,
            object : NsdManager.ResolveListener {
                override fun onServiceResolved(resolu: NsdServiceInfo) {
                    if (suite.isActive) suite.resume(resolu)
                }

                override fun onResolveFailed(service: NsdServiceInfo, code: Int) {
                    if (suite.isActive) suite.resume(null)
                }
            },
        )
    }
