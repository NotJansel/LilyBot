package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.menus.EphemeralSelectMenuContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.readBytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class Modrinth : Extension() {
	override val name = "modrinth"

	override suspend fun setup() {
		publicSlashCommand {
			name = "modrinth"
			description = "Search Modrinth for a mod!"

			action {
				respond {
					content = "Use the menu below to narrow your search"
					components {
						ephemeralSelectMenu(0) {
							placeholder = "Adjust your search parameters"
							maximumChoices = 1
							option("Edit search keyword", "keyword") {
								description = "Change the search term for your search"
							}
							option("Edit category filter", "category") {
								description = "Change which categories you want to limit your search to"
							}
							option("Edit environment filter", "environment") {
								description = "Change which environment(s) you want to limit your search to"
							}
							option("Edit loader filter", "loader") {
								description = "Change which mod loader(s) you want to limit your search to"
							}
							option("Edit version filter", "version") {
								description = "Change which version(s) you want to limit your search to"
							}
							option("Edit license filter", "license") {
								description = "Change which license(s) you want to limit your search to"
							}
							option("Edit project type filter", "type") {
								description = "Change which project type(s) you want to limit your search to"
							}

							action {
								when (this.selected[0]) {
									"keyword" -> respond { content = "keyword modal" }
									"category" -> createFilterMenu("category")
									"environment" -> createFilterMenu("environment")
									"loader" -> createFilterMenu("loader")
									"version" -> createFilterMenu("version")
									"license" -> createFilterMenu("license")
								}
							}
						}
					}
				}
			}
		}
	}

	private suspend fun EphemeralSelectMenuContext.createFilterMenu(filterType: String) {
		val filterOptions = when (filterType) {
			"category" -> getModCategories()
			"environment" -> mutableListOf("server", "client")
			"loader" -> getModLoaders()
			"version" -> getMinecraftVersions()
			"license" -> getLicenses()
			else -> mutableListOf("something has gone very wrong") // Is there a better way to do this?
		}

		respond {
			components {
				ephemeralSelectMenu {
					maximumChoices = filterOptions.size
					placeholder = "Filter by $filterType"
					filterOptions.forEach {
						option(it, it)
					}
					action {
						searchModrinth(filterType, this.selected)
					}
				}
			}
		}
	}

	private fun searchModrinth(filterType: String, selectedOptions: List<String>) {
		// todo
		// below this is just for detekt
		println(filterType)
		println(selectedOptions)
	}

	private suspend fun getModCategories(): MutableList<String> {
		val client = HttpClient()
		val response = client.request("https://api.modrinth.com/v2/tag/category")
			.readBytes().decodeToString()
		client.close()

		val json = Json { ignoreUnknownKeys = true }
		val stringResponseArray = json.decodeFromString<List<CategoryData>>(response)

		val modCategories = mutableListOf<String>()
		stringResponseArray.forEach {
			if (it.projectType == "mod") {
				modCategories.add(it.name)
			}
		}
		return modCategories
	}

	private suspend fun getModLoaders(): MutableList<String> {
		val client = HttpClient()
		val response = client.request("https://api.modrinth.com/v2/tag/loader")
			.readBytes().decodeToString()
		client.close()

		val json = Json { ignoreUnknownKeys = true }
		val stringResponseArray = json.decodeFromString<List<LoaderData>>(response)

		val modLoaders = mutableListOf<String>()
		stringResponseArray.forEach {
			if ("mod" in it.supportedProjectTypes) {
				modLoaders.add(it.name)
			}
		}
		return modLoaders
	}

	// returns only releases
	private suspend fun getMinecraftVersions(): MutableList<String> {
		val client = HttpClient()
		val response = client.request("https://api.modrinth.com/v2/tag/game_version")
			.readBytes().decodeToString()
		client.close()

		val json = Json { ignoreUnknownKeys = true }
		val stringResponseArray = json.decodeFromString<List<VersionData>>(response)

		val minecraftVersions = mutableListOf<String>()
		stringResponseArray.forEach {
			if (it.versionType == "release") {
				minecraftVersions.add(it.version)
			}
		}
		return minecraftVersions
	}

	private suspend fun getLicenses(): MutableList<String> {
		val client = HttpClient()
		val response = client.request("https://api.modrinth.com/v2/tag/license")
			.readBytes().decodeToString()
		client.close()

		val json = Json { ignoreUnknownKeys = true }
		val stringResponseArray = json.decodeFromString<List<LicenseData>>(response)

		val licenses = mutableListOf<String>()
		stringResponseArray.forEach {
			licenses.add(it.short)
			println(it.short)
		}
		return licenses
	}

	@Serializable
	data class CategoryData(
		val icon: String,
		val name: String,
		@SerialName("project_type") val projectType: String
	)

	@Serializable
	data class LoaderData(
		val icon: String,
		val name: String,
		@SerialName("supported_project_types") val supportedProjectTypes: MutableList<String>
	)

	@Serializable
	data class VersionData(
		val version: String,
		@SerialName("version_type") val versionType: String,
		val date: String,
		val major: Boolean
	)

	@Serializable
	data class LicenseData(
		val short: String,
		val name: String
	)
}
