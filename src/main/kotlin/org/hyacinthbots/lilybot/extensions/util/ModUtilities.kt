package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_WHITE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalColour
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.database.collections.LogUploadingBlacklistCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.StatusCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs
import org.hyacinthbots.lilybot.utils.trimmedContents
import org.hyacinthbots.lilybot.utils.updateDefaultPresence
import kotlin.time.Duration.Companion.seconds

/**
 * This class contains a few utility commands that can be used by moderators. They all require a guild to be run.
 *
 * @since 3.1.0
 */
class ModUtilities : Extension() {
	override val name = "mod-utilities"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		/**
		 * Say Command
		 * @author NoComment1105, tempest15
		 * @since 2.0
		 */
		ephemeralSlashCommand(::SayArgs) {
			name = "say"
			description = "Say something through Lily."

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
			}
			action {
				val targetChannel: GuildMessageChannel?
				try {
					targetChannel =
						if (arguments.channel != null) {
							guild!!.getChannelOf(arguments.channel!!.id)
						} else {
							channel.asChannelOf()
						}
				} catch (e: EntityNotFoundException) {
					respond { content = "Channel not found." }
					return@action
				}
				val createdMessage: Message

				try {
					if (arguments.embed) {
						createdMessage = targetChannel.createEmbed {
							color = arguments.color
							description = arguments.message
							if (arguments.timestamp) {
								timestamp = Clock.System.now()
							}
						}
					} else {
						createdMessage = targetChannel.createMessage {
							content = arguments.message
						}
					}
				} catch (e: KtorRequestException) {
					respond { content = "Lily does not have permission to send messages in this channel." }
					return@action
				}

				respond { content = "Message sent." }

				val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!) ?: return@action
				utilityLog.createMessage {
					embed {
						title = "Say command used"
						description = "```${arguments.message}```"
						field {
							name = "Channel:"
							value = targetChannel.mention
							inline = true
						}
						field {
							name = "Type:"
							value = if (arguments.embed) "Embed" else "Message"
							inline = true
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						if (arguments.embed) {
							color = arguments.color
							field {
								name = "Color:"
								value = arguments.color.toString()
								inline = true
							}
						} else {
							color = DISCORD_BLACK
						}
					}
					components {
						linkButton {
							label = "Jump to message"
							url = createdMessage.getJumpUrl()
						}
					}
				}
			}
		}

		/**
		 * Message editing command
		 *
		 * @since 3.3.0
		 */
		ephemeralSlashCommand(::SayEditArgs) {
			name = "edit-say"
			description = "Edit a message created by /say"

			check {
				anyGuild()
				hasPermission(Permission.ModerateMembers)
				requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
			}

			action {
				// The channel the message was sent in. Either the channel provided, or if null, the channel the
				// command was executed in.
				val channelOfMessage = if (arguments.channelOfMessage != null) {
					guild!!.getChannel(arguments.channelOfMessage!!.id) as MessageChannelBehavior
				} else {
					channel
				}
				val message: Message

				try {
					message = channelOfMessage.getMessage(arguments.messageToEdit)
				} catch (e: KtorRequestException) { // In the event of the message being in a channel the bot can't see
					respond {
						content = "Sorry, I can't properly access this message."
					}
					return@action
				} catch (e: EntityNotFoundException) { // In the event of the message already being deleted.
					respond {
						content = "Sorry, I can't find this message."
					}
					return@action
				}

				val originalContent = message.content
				// The messages that contains the embed that is going to be edited. If the message has no embed, or
				// it's not by LilyBot, it returns
				if (message.embeds.isEmpty()) {
					if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
						respond { content = "I did not send this message, I cannot edit this!" }
						return@action
					} else if (arguments.newContent == null) {
						respond { content = "Please specify a new message content" }
						return@action
					} else if (arguments.newContent != null && arguments.newContent!!.length > 1024) {
						respond {
							content =
								"Maximum embed length reached! Your embed character length cannot be more than 1024 " +
										"characters, due to Discord limitations"
						}
						return@action
					}

					message.edit { content = arguments.newContent }

					respond { content = "Message edited" }

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
						?: return@action
					utilityLog.createMessage {
						embed {
							title = "Say message edited"
							field {
								name = "Original Content"
								value = "```${originalContent.trimmedContents(500)}```"
							}
							field {
								name = "New Content"
								value = "```${arguments.newContent.trimmedContents(500)}```"
							}
							footer {
								text = "Edited by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
							color = DISCORD_WHITE
							timestamp = Clock.System.now()
						}
						components {
							linkButton {
								label = "Jump to message"
								url = message.getJumpUrl()
							}
						}
					}
				} else {
					if (message.author!!.id != this@ephemeralSlashCommand.kord.selfId) {
						respond { content = "I did not send this message, I cannot edit this!" }
						return@action
					}

					// The old description and color to the embed. We get it here before we start changing it.
					val oldContent = message.embeds[0].description
					val oldColor = message.embeds[0].color
					val oldTimestamp = message.embeds[0].timestamp

					message.edit {
						embed {
							description = arguments.newContent ?: oldContent
							color = arguments.newColor ?: oldColor
							timestamp = when (arguments.timestamp) {
								true -> message.timestamp
								false -> null
								null -> oldTimestamp
							}
						}
					}

					respond { content = "Embed updated" }

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
						?: return@action
					utilityLog.createMessage {
						embed {
							title = "Say message edited"
							field {
								name = "Original content"
								// The old content, if null none
								value = "```${oldContent ?: "none"}```"
							}
							field {
								name = "New content"
								// The new content, if null the old content, if null none
								value = "```${arguments.newContent ?: oldContent ?: "none"}```"
							}
							field {
								name = "Old color"
								value = oldColor.toString()
							}
							field {
								name = "New color"
								value =
									if (arguments.newColor != null) arguments.newColor.toString() else oldColor.toString()
							}
							field {
								name = "Has Timestamp"
								value = when (arguments.timestamp) {
									true -> "True"
									false -> "False"
									else -> "Original"
								}
							}
							footer {
								text = "Edited by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
							timestamp = Clock.System.now()
							color = DISCORD_WHITE
						}
						components {
							linkButton {
								label = "Jump to message"
								url = message.getJumpUrl()
							}
						}
					}
				}
			}
		}

		/**
		 * Presence Command
		 * @author IMS
		 * @since 2.0
		 */
		ephemeralSlashCommand(::PresenceArgs) {
			name = "status"
			description = "Set Lily's current presence/status."

			guild(TEST_GUILD_ID)

			ephemeralSubCommand(::PresenceArgs) {
				name = "set"
				description = "Set a custom status for Lily."
				guild(TEST_GUILD_ID)

				check {
					hasPermission(Permission.Administrator)
					requiredConfigs(ConfigOptions.MODERATION_ENABLED, ConfigOptions.ACTION_LOG)
				}

				action {
					val config = ModerationConfigCollection().getConfig(guildFor(event)!!.id)!!
					val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel!!)

					// Update the presence in the action
					this@ephemeralSlashCommand.kord.editPresence {
						status = PresenceStatus.Online
						playing(arguments.presenceArgument)
					}

					// Store the new presence in the database for if there is a restart
					StatusCollection().setStatus(arguments.presenceArgument)

					respond { content = "Presence set to `${arguments.presenceArgument}`" }

					actionLog.createEmbed {
						title = "Presence changed"
						description = "Lily's presence has been set to `${arguments.presenceArgument}`"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						color = DISCORD_BLACK
					}
				}
			}

			ephemeralSubCommand {
				name = "reset"
				description = "Reset Lily's presence to the default status."
				guild(TEST_GUILD_ID)

				check {
					hasPermission(Permission.Administrator)
					requiredConfigs(ConfigOptions.MODERATION_ENABLED, ConfigOptions.ACTION_LOG)
				}

				action {
					// Store the new presence in the database for if there is a restart
					StatusCollection().setStatus(null)

					updateDefaultPresence()
					val guilds = this@ephemeralSlashCommand.kord.guilds.toList().size

					respond { content = "Presence set to default" }

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
						?: return@action
					utilityLog.createEmbed {
						title = "Presence changed"
						description = "Lily's presence has been set to default."
						field {
							value = "Watching over $guilds servers."
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						color = DISCORD_BLACK
					}
				}
			}
		}

		unsafeSlashCommand {
			name = "reset"
			description = "'Resets' Lily for this guild by deleting all database information relating to this guild"

			initialResponse = InitialSlashCommandResponse.None

			requirePermission(Permission.Administrator) // Hide this command from non-administrators

			check {
				anyGuild()
				hasPermission(Permission.Administrator)
			}

			action {
				val modal = event.interaction.modal("Reset data for this guild", "resetModal") {
					actionRow {
						textInput(TextInputStyle.Short, "confirmation", "Confirm reset") {
							placeholder = "Type 'yes' to confirm"
						}
					}
				}

				val interaction =
					modal.kord.waitFor<ModalSubmitInteractionCreateEvent>(120.seconds.inWholeMilliseconds) {
						interaction.modalId == "resetModal"
					}?.interaction

				if (interaction == null) {
					modal.createEphemeralFollowup { content = "Reset interaction timed out" }
					return@action
				}

				val confirmation = interaction.textInputs["confirmation"]!!.value!!
				val modalResponse = interaction.deferEphemeralResponse()

				if (confirmation.lowercase() != "yes") {
					modalResponse.respond { content = "Confirmation failure. Reset cancelled" }
					return@action
				}

				var response: EphemeralMessageInteractionResponse? = null

				response = modalResponse.respond {
					content =
						"Are you sure you want to reset the database? This will remove all data associated with " +
								"this guild from Lily's database. This includes configs, user-set reminders, tags and more." +
								"This action is **irreversible** and the data **cannot** be recovered."

					components {
						ephemeralButton(0) {
							label = "I'm sure"
							style = ButtonStyle.Danger

							action {
								response?.edit {
									content = "Database reset!"
									components { removeAll() }
								}

								guild?.getChannelOf<GuildMessageChannel>(
									ModerationConfigCollection().getConfig(guild!!.id)?.channel ?: guild!!.asGuild()
										.getSystemChannel()!!.id
								)?.createMessage {
									embed {
										title = "Database Reset!"
										description = "All data associated with this guild has been removed."
										timestamp = Clock.System.now()
										color = DISCORD_BLACK
									}
								}

								// Reset
								LoggingConfigCollection().clearConfig(guild!!.id)
								ModerationConfigCollection().clearConfig(guild!!.id)
								SupportConfigCollection().clearConfig(guild!!.id)
								UtilityConfigCollection().clearConfig(guild!!.id)
								GalleryChannelCollection().removeAll(guild!!.id)
								GithubCollection().removeDefaultRepo(guild!!.id)
								LogUploadingBlacklistCollection().clearBlacklist(guild!!.id)
								ReminderCollection().removeGuildReminders(guild!!.id)
								RoleMenuCollection().removeAllRoleMenus(guild!!.id)
								TagsCollection().clearTags(guild!!.id)
								ThreadsCollection().removeGuildThreads(guild!!.id)
								WarnCollection().clearWarns(guild!!.id)
								WelcomeChannelCollection().removeWelcomeChannelsForGuild(guild!!.id, kord)
							}
						}

						ephemeralButton(0) {
							label = "Nevermind"
							style = ButtonStyle.Secondary

							action {
								response?.edit {
									content = "Reset cancelled"
									components { removeAll() }
								}
							}
						}
					}
				}
			}
		}

		/**
		 * Update the presence to reflect the new number of guilds, if the presence is set to "default"
		 *
		 * @author NoComment1105
		 * @since 3.4.5
		 */
		event<GuildCreateEvent> {
			action {
				updateDefaultPresence()
			}
		}

		/**
		 * Update the presence to reflect the new number of guilds, if the presence is set to "default"
		 *
		 * @author NoComment1105
		 * @since 3.4.5
		 */
		event<GuildDeleteEvent> {
			action {
				updateDefaultPresence()
			}
		}
	}

	inner class SayArgs : Arguments() {
		/** The message the user wishes to send. */
		val message by string {
			name = "message"
			description = "The text of the message to be sent."

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
					.replace("\n", "\n")
			}
		}

		/** The channel to aim the message at. */
		val channel by optionalChannel {
			name = "channel"
			description = "The channel the message should be sent in."
		}

		/** Whether to embed the message or not. */
		val embed by defaultingBoolean {
			name = "embed"
			description = "If the message should be sent as an embed."
			defaultValue = false
		}

		/** If the embed should have a timestamp. */
		val timestamp by defaultingBoolean {
			name = "timestamp"
			description = "If the message should be sent with a timestamp. Only works with embeds."
			defaultValue = true
		}

		/** What color the embed should be. */
		val color by defaultingColor {
			name = "color"
			description = "The color of the embed. Can be either a hex code or one of Discord's supported colors. " +
					"Embeds only"
			defaultValue = DISCORD_BLURPLE
		}
	}

	inner class SayEditArgs : Arguments() {
		/** The ID of the embed to edit. */
		val messageToEdit by snowflake {
			name = "message-to-edit"
			description = "The ID of the message you'd like to edit"
		}

		/** The new content of the embed. */
		val newContent by optionalString {
			name = "new-content"
			description = "The new content of the message"

			mutate {
				it?.replace("\\n", "\n")
					?.replace("\n ", "\n")
					?.replace("\n", "\n")
			}
		}

		/** The new color for the embed. */
		val newColor by optionalColour {
			name = "new-color"
			description = "The new color of the embed. Embeds only"
		}

		/** The channel the embed was originally sent in. */
		val channelOfMessage by optionalChannel {
			name = "channel-of-message"
			description = "The channel of the message"
		}

		/** Whether to add the timestamp of when the message was originally sent or not. */
		val timestamp by optionalBoolean {
			name = "timestamp"
			description = "Whether to timestamp the embed or not. Embeds only"
		}
	}

	inner class PresenceArgs : Arguments() {
		/** The new presence set by the command user. */
		val presenceArgument by string {
			name = "presence"
			description = "The new value Lily's presence should be set to"
		}
	}
}
