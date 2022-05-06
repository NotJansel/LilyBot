package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.menus.EphemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.readBytes
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import org.quiltmc.mappings_hasher.com.google.gson.annotations.SerializedName

class Modrinth : Extension() {
	override val name = "modrinth"

	override suspend fun setup() {
		/**
		 * A command to dynamically search Modrinth from Discord
		 *
		 * @author tempest15
		 * @since 3.2.0
		 */
		publicSlashCommand(::ModrinthArgs) {
			name = "modrinth"
			description = "Search Modrinth in Discord! Returns the top 5 results." // todo Change results value

			action {
				var menu: EphemeralSelectMenu
				var response = respond {
					embed {
						color = DISCORD_GREEN
						timestamp = Clock.System.now()
						title = "Searching Modrinth"
						description = if (arguments.query == null) {
							"Please enter a search term!"
						} else {
							"Searching Modrinth for ${arguments.query}..."
						}
					}
					components {
						menu = ephemeralSelectMenu(0) {
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
							option("Edit license filter", "license") {
								description = "Change which license(s) you want to limit your search to"
							}
							option("Edit project type filter", "type") {
								description = "Change which project type(s) you want to limit your search to"
							}

							action {
								val selection = this.selected
								respond { content = "You picked $selection" }
							}
						}
					}
				}

				// Create an embed with various buttons for search, filtering, and browsing.
				// An embed should be able to be created with no arguments selected.
				// If button inputs occur, search Modrinth and reconstruct the embed.

				searchModrinth(
					SearchData(
						null,
						null,
						null,
						null,
						null,
						null
					)
				)
			}
		}
	}

	/**
	 * A function that takes search parameters and returns the top 5 results from Modrinth.
	 *
	 * @param searchInput The [SearchData] containing the parameters of your search
	 * @return The top 5 results from Modrinth as [ResponseData]
	 * @author tempest15
	 * @since 3.2.0
	 */
	private suspend fun searchModrinth(searchInput: SearchData) {
		val query = null // todo process the SearchData into Modrinth format

		println(searchInput.category)

		val client = HttpClient()
		val response = client.request("https://api.modrinth.com/v2/search?limit=5&query=$query")
			.readBytes().decodeToString()
		client.close()

		val json = Json { ignoreUnknownKeys = true }

		// todo I don't think this handles errors at all
		val decodedResponse = json.decodeFromString<ResponseData>(response)
		println(decodedResponse.hits)
		val mod = json.decodeFromJsonElement<ModData>(decodedResponse.hits)
		println(mod.author)
	}

	inner class ModrinthArgs : Arguments() {
		val query by optionalString {
			name = "query"
			description = "The query to search for, most likely a project name."
		}
	}

	@Serializable
	data class ResponseData(
		val hits: JsonArray,
		val limit: Int,
		val offset: Int,
		@SerializedName("total_hits") val totalHits: Int
	)

	@Serializable
	data class ModData(
		val author: String,
		val categories: JsonArray,
		@SerializedName("client_side") val clientSide: String,
		@SerializedName("date_created") val dateCreated: String,
		@SerializedName("date_modified") val dateModified: String,
		val description: String,
		val downloads: Int,
		val follows: Int,
		val gallery: JsonArray,
		@SerializedName("icon_url") val iconUrl: String,
		@SerializedName("latest_version") val latestVersion: String,
		val license: String,
		@SerializedName("project_id") val projectId: String,
		@SerializedName("project_type") val projectType: String,
		@SerializedName("server_side") val serverSide: String,
		val slug: String,
		val title: String,
		val versions: JsonArray
	)

	// todo Many of these likely don't need to be null. Further in investigation is needed.
	data class SearchData(
		val query: String?,
		val category: CategoryData?,
		val environment: EnvironmentData?,
		val loader: LoaderData?,
		val licence: LicenseData?,
		val projectType: ProjectTypeData?
	)

	data class CategoryData(
		val adventure: Boolean?,
		val cursed: Boolean?,
		val decoration: Boolean?,
		val equipment: Boolean?,
		val food: Boolean?,
		val library: Boolean?,
		val magic: Boolean?,
		val misc: Boolean?,
		val optimization: Boolean?,
		val storage: Boolean?,
		val technology: Boolean?,
		val utility: Boolean?,
		val worldgen: Boolean?,
	)

	data class EnvironmentData(
		val server: Boolean?,
		val client: Boolean?,
		val universal: Boolean?
	)

	data class LoaderData(
		val forge: Boolean?,
		val fabric: Boolean?,
		val quilt: Boolean?
	)

	// todo Add a version data class
	// Use the version manifest https://minecraft.fandom.com/wiki/Version_manifest.json and
	// https://launchermeta.mojang.com/mc/game/version_manifest_v2.json to validate the version.

	data class LicenseData(
		val custom: Boolean?,
		val lgpl: Boolean?,
		val apache: Boolean?,
		val bsd2clause: Boolean?,
		val bsd3clause: Boolean?,
		val bsl: Boolean?,
		val cc0: Boolean?,
		val unlicense: Boolean?,
		val mpl: Boolean?,
		val mit: Boolean?,
		val arr: Boolean?,
		val lgpl3: Boolean?
	)

	data class ProjectTypeData(
		val mod: Boolean?,
		val modpack: Boolean?
	)
}
