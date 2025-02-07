package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.LogUploadingBlacklistData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains functions for interaction with the [log uploading blacklist database][LogUploadingBlacklistData].
 * This class contains functions for setting the blacklist, removing the blacklist, getting, clearing and checking for
 * presence in the blacklist.
 *
 * @since 4.0.0
 * @see setLogUploadingBlacklist
 * @see removeLogUploadingBlacklist
 * @see getLogUploadingBlacklist
 * @see clearBlacklist
 * @see isChannelInUploadBlacklist
 */
class LogUploadingBlacklistCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<LogUploadingBlacklistData>()

	/**
	 * Sets a channel as blacklisted for uploading logs.
	 *
	 * @param inputGuildId The guild the command was run in
	 * @param inputChannelId The channel to disable uploading for
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun setLogUploadingBlacklist(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.insertOne(LogUploadingBlacklistData(inputGuildId, inputChannelId))

	/**
	 * Removes a channel from the blacklist for uploading logs.
	 *
	 * @param inputGuildId The guild the command was run in
	 * @param inputChannelId The channel to re-enable uploading for
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun removeLogUploadingBlacklist(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.deleteOne(
			LogUploadingBlacklistData::guildId eq inputGuildId,
			LogUploadingBlacklistData::channelId eq inputChannelId
		)

	/**
	 * Checks the log uploading blacklist for the given [inputChannelId].
	 *
	 * @param inputGuildId The guild to get the [inputChannelId] is in
	 * @param inputChannelId The channel to check is blacklisted or not
	 * @return The data for the channel or null
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun isChannelInUploadBlacklist(
		inputGuildId: Snowflake,
		inputChannelId: Snowflake
	): LogUploadingBlacklistData? = collection.findOne(
		LogUploadingBlacklistData::guildId eq inputGuildId,
		LogUploadingBlacklistData::channelId eq inputChannelId
	)

	/**
	 * Gets the log uploading blacklist for the given [inputGuildId].
	 *
	 * @param inputGuildId The guild to get the blacklist for
	 * @return The list of blacklisted channels for the given guild
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun getLogUploadingBlacklist(inputGuildId: Snowflake): List<LogUploadingBlacklistData> =
		collection.find(LogUploadingBlacklistData::guildId eq inputGuildId).toList()

	/**
	 * Removes all data of the log upload blacklist for a given guild.
	 *
	 * @param inputGuildId The guild to clear the data from
	 * @author NoComment1105
	 * @since 4.1.0
	 */
	suspend inline fun clearBlacklist(inputGuildId: Snowflake) =
		collection.deleteMany(LogUploadingBlacklistData::guildId eq inputGuildId)
}
