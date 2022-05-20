package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
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
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.forEach
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class Modrinth : Extension() {
	override val name = "modrinth"

	override suspend fun setup() {
		publicSlashCommand(::ModrinthArgs) {
			name = "modrinth"
			description = "Search Modrinth for a mod!" // todo look into expanding to other project types

			action {
				var searchFilters = SearchData(arguments.keyword, mutableMapOf(Pair("", "")))

				respond {
					content = "Use the menu below to narrow your search"
					components {
						ephemeralSelectMenu(0) {
							placeholder = "Adjust your search parameters"
							maximumChoices = 1
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
									"category" -> searchFilters = createFilterMenu(
										"category",
										getModCategories(),
										searchFilters
									)
									"environment" -> searchFilters = createFilterMenu(
										"environment",
										mutableListOf("server", "client"),
										searchFilters
									)
									"loader" -> searchFilters = createFilterMenu(
										"loader",
										getModLoaders(),
										searchFilters
									)
									"version" -> searchFilters = createFilterMenu(
										"version",
										// only use 25 results due to limit of select menus
										getMinecraftVersions().subList(0, 24),
										searchFilters
									)
									"license" -> searchFilters = createFilterMenu(
										"license",
										getLicenses(),
										searchFilters
									)
								}
							}
						}
					}
				}
			}
		}
	}

	private suspend fun searchModrinth(currentFilter: SearchData) {
		val route = "https://api.modrinth.com/v2/search?limit=5"

		val client = HttpClient()
		val response = client.request(route)
			.readBytes().decodeToString()
		client.close()

		val json = Json { ignoreUnknownKeys = true }
		val decodedResponse = json.decodeFromString<SearchResponseData>(response)

		// for detekt
		println(decodedResponse)
		println(currentFilter)
	}

	private suspend fun EphemeralSelectMenuContext.createFilterMenu(
		filterType: String,
		filterOptions: MutableList<String>,
		currentFilter: SearchData
	): SearchData {
		respond {
			components {
				ephemeralSelectMenu {
					maximumChoices = filterOptions.size
					placeholder = "Filter by $filterType"
					filterOptions.forEach {
						option(it, it)
					}
					action {
						this.selected.forEach {
							currentFilter.facets[it] = filterType
						}
						searchModrinth(currentFilter)
						// todo this is where the paginator needs to be updated
					}
				}
			}
		}
		return currentFilter // return it so any other functions called can access it
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

	// todo This is a temporary solution for keyword. Ideally, it will be modals in the future.
	inner class ModrinthArgs : Arguments() {
		val keyword by string {
			name = "keyword"
			description = "The keyword to base your search off. Due to technical limitations, this cannot be edited."
		}
	}

	data class SearchData(
		val query: String,
		val facets: MutableMap<String, String>, // the key is the facet and the value is the facet type
	)

	@Serializable
	data class SearchResponseData(
		val hits: List<ProjectData>,
		val offset: Int,
		val limit: Int,
		@SerialName("total_hits") val totalHits: Int
	)

	@Serializable
	data class ProjectData(
		val slug: String,
		val title: String,
		val description: String,
		val categories: MutableList<String>,
		@SerialName("client_side") val clientSide: String,
		@SerialName("server_side") val serverSide: String,
		@SerialName("source_url") val sourceURL: String? = null,
		@SerialName("discord_url") val discordURL: String? = null,
		@SerialName("project_type") val projectType: String,
		val downloads: Int,
		@SerialName("icon_url") val iconURL: String?,
		val license: String?
	)

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
