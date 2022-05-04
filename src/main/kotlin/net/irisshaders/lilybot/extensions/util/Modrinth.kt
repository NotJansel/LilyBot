package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalStringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
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

class Modrinth : Extension() {
	override val name = "modrinth"

	override suspend fun setup() {
		/**
		 * Commands for accessing/previewing projects from Modrinth
		 *
		 * @author tempest15
		 * @since 3.2.0
		 */
		publicSlashCommand(::ModrinthArgs) {
			name = "modrinth"
			description = "Search Modrinth in Discord! Returns the top 5 results."

			action {
				// todo check the validity of the arguments.version

				val url: StringBuilder = StringBuilder("https://modrinth.com/api/v2/search?")
				val searchTerms = mutableMapOf(
					"category" to arguments.category,
					"version" to arguments.version,
					"license" to arguments.license,
					"project_type" to arguments.type,
				)

				// todo this is a bad way to process this
				searchTerms.forEach {
					if (it.value != null) {
						url.append("${it.key}=${it.value}&")
					}
				}

				val client = HttpClient()
				val response = client.request("https://api.modrinth.com/v2/search?limit=1&query=${arguments.query}")
					.readBytes().decodeToString()
				client.close()

				val json = Json { ignoreUnknownKeys = true }

				// todo I don't think this handles errors at all
				val decodedResponse = json.decodeFromString<ResponseData>(response)
				val project = json.decodeFromString<ModData>(decodedResponse.hits[0].toString())

				respond {
					// todo this embed can be way over the limit
					embed {
						color = DISCORD_GREEN
						timestamp = Clock.System.now()

						title = "Modrinth search results"
						description = "**Search term:** `${arguments.query}`" +
								"\n**Results found:** ${decodedResponse.total_hits}"
						field {
							name = "${project.title} by ${project.author}"
							value = "**Type:** ${project.project_type}\n" +
									"**Description:** ${project.description}"
						}
						field {
							name = "Project URL"
							value = "https://modrinth.com/${project.project_type}/${project.project_id}"
						}

						field {
							name = "Categories"
							value = project.categories.joinToString(", ")
							inline = true
						}

						field {
							name = "Environment"
							value = "Client side: ${project.client_side}\nServer side: ${project.server_side}"
							inline = true
						}

						field {
							name = "Statistics"
							value = "**Downloads:** ${project.downloads}\n**Followers:** ${project.follows}"
							inline = true
						}

						field {
							name = "Versions"
							value = project.versions.joinToString(", ")
							inline = true
						}

						field {
							name = "Updates"
							value = "**Date created:** ${project.date_created}\n" +
									"**Last modified:** ${project.date_modified}\n" +
									"**Latest version:** ${project.latest_version}"
						}
					}
				}
			}
		}
	}

	// todo All of these are or. It should be possible to search using multiple values for a single argument.
	inner class ModrinthArgs : Arguments() {
		val query by string {
			name = "query"
			description = "The query to search for, most likely a project name"
		}

		// todo this should be a select menu instead
		val category by optionalStringChoice {
			name = "category"
			description = "The category you want to limit your search to"
			choices = mutableMapOf(
				"adventure" to "adventure",
				"cursed" to "cursed",
				"decoration" to "decoration",
				"equipment" to "equipment",
				"food" to "food",
				"library" to "library",
				"magic" to "magic",
				"misc" to "misc",
				"optimization" to "optimization",
				"storage" to "storage",
				"technology" to "technology",
				"utility" to "utility",
				"worldgen" to "worldgen",
			)
		}

		// todo loader is classed as part of category, this needs proper handling
		val loader by optionalStringChoice {
			name = "loader"
			description = "The mod loader you want to limit your search to"
			choices = mutableMapOf(
				"forge" to "forge",
				"fabric" to "fabric",
				"quilt" to "quilt",
			)
		}

		// todo environment is also a category
		val environment by optionalStringChoice {
			name = "environment"
			description = "The environment you want to limit your search to. Sever, client, or universal."
			choices = mutableMapOf(
				"server" to "server",
				"client" to "client",
				"universal" to "universal",
			)
		}

		val version by optionalString {
			name = "version"
			description = "The Minecraft version to limit your search to. Snapshots are supported."
		}

		// todo this should also be a select menu
		val license by optionalStringChoice {
			name = "license"
			description = "The license you want to limit your search to."
			choices = mutableMapOf(
				"custom" to "custom",
				"lgpl" to "lgpl",
				"apache" to "apache",
				"bsd-2-clause" to "bsd-2-clause",
				"bsd-3-clause" to "bsd-3-clause",
				"bsl" to "bsl",
				"cc0" to "cc0",
				"unlicense" to "unlicense",
				"mpl" to "mpl",
				"mit" to "mit",
				"arr" to "arr",
				"lgpl-3" to "lgpl-3",
			)
		}

		val type by optionalStringChoice {
			name = "type"
			description = "The project type you want to limit your search to."
			choices = mutableMapOf(
				"mod" to "mod",
				"modpack" to "modpack",
			)
		}
	}

	@Serializable
	data class ResponseData(
		val hits: JsonArray,
		val limit: Int,
		val offset: Int,
		val total_hits: Int
	)

	@Serializable
	data class ModData(
		val author: String,
		val categories: JsonArray,
		val client_side: String,
		val date_created: String,
		val date_modified: String,
		val description: String,
		val downloads: Int,
		val follows: Int,
		val gallery: JsonArray,
		val icon_url: String,
		val latest_version: String,
		val license: String,
		val project_id: String,
		val project_type: String,
		val server_side: String,
		val slug: String,
		val title: String,
		val versions: JsonArray
	)
}
