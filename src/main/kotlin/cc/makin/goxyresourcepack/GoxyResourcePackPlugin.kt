package cc.makin.goxyresourcepack

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import pl.goxy.minecraft.api.GoxyApi
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class GoxyResourcePackPlugin : JavaPlugin(), Listener {
    private lateinit var resourcePackRequest: ResourcePackRequest

    override fun onEnable() {
        this.saveDefaultConfig()

        try {
            this.resourcePackRequest = this.resourcePackRequestFromConfig()
        } catch (ex: Throwable) {
            if (ex is InvalidConfigurationException) {
                this.logger.severe("Invalid configuration: ${ex.message}. Disabling plugin.")
            } else {
                this.logger.severe("Failed to create resource pack request: ${ex.message}. Disabling plugin.")
            }
            this.server.pluginManager.disablePlugin(this)
            return
        }

        this.server.pluginManager.registerEvents(this, this)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun sendResourcePack(event: PlayerJoinEvent) {
        val firstServer = GoxyApi.getPlayerStorage().getPlayer(event.player.uniqueId)?.previousServer == null
        if (firstServer) {
            event.player.sendResourcePacks(this.resourcePackRequest)
        }
    }

    private fun resourcePackRequestFromConfig() = run {
        val packConfig = this.config.getConfigurationSection("resource-pack")
            ?: throw InvalidConfigurationException("resource pack section is not set in the config file")
        val resourcePackUrl = (packConfig.getString("url")
            ?: throw InvalidConfigurationException("resource pack URL is not set in the config file"))
            .runCatching { URI(this) }
            .getOrElse { throw InvalidConfigurationException("invalid resource pack URL: ${it.message}") }
        val prompt = (packConfig.getString("prompt")
            ?: run {
                this.logger.warning("Resource pack prompt is not set in the config file. Using default prompt.")
                this.config.defaults!!.getString("resource-pack.prompt")!!
            })
            .let { MiniMessage.miniMessage().deserialize(it) }
        val required = packConfig.getBoolean("required")
        val hash = packConfig.getString("hash")

        ResourcePackInfo.resourcePackInfo()
            .uri(resourcePackUrl)
            .buildWithOptionalHash(hash)
            .thenApply { packInfo ->
                ResourcePackRequest.resourcePackRequest()
                    .packs(packInfo)
                    .prompt(prompt)
                    .required(required)
                    .build()
            }
            .runCatching { get(30, TimeUnit.SECONDS) }
            .getOrElse {
                when (it) {
                    is ExecutionException -> throw InvalidConfigurationException("failed to create resource pack request: ${it.cause?.message}")
                    is TimeoutException -> throw InvalidConfigurationException("compute resource pack hash timeout")
                    else -> throw it
                }
            }
    }

    private fun ResourcePackInfo.Builder.buildWithOptionalHash(hash: String?) =
        if (hash != null) {
            CompletableFuture.completedFuture(this.hash(hash).build())
        } else {
            logger.info("Computing resource pack hash...")
            val now = Instant.now()
            this.computeHashAndBuild()
                .whenComplete { result, throwable ->
                    if (throwable == null) {
                        logger.info(
                            "Computed resource pack hash in " +
                                    Duration.between(now, Instant.now()).toMillis() + " ms."
                        )
                    }
                }
        }
}
