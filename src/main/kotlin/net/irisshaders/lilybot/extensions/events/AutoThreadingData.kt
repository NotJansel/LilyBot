package net.irisshaders.lilybot.extensions.events

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

// This is a temporary file to make integration of this branch into the config rewrite easier
@Serializable
data class AutoThreadingData(
	val channelId: Snowflake,
	val roleId: Snowflake?,
	val creationMessage: String?,
	val allowDuplicates: Boolean,
	val archive: Boolean,
	val namingScheme: AutoThreading.ThreadNamingSchemes
)

suspend inline fun getAutoThread(inputChannelId: Snowflake): AutoThreadingData? {
	val collection = database.getCollection<AutoThreadingData>()
	return collection.findOne(AutoThreadingData::channelId eq inputChannelId)
}

suspend inline fun setAutoThread(inputAutoThreadData: AutoThreadingData) {
	val collection = database.getCollection<AutoThreadingData>()
	collection.deleteOne(AutoThreadingData::channelId eq inputAutoThreadData.channelId)
	collection.insertOne(inputAutoThreadData)
}

suspend inline fun deleteAutoThread(inputChannelId: Snowflake) {
	val collection = database.getCollection<AutoThreadingData>()
	collection.deleteOne(AutoThreadingData::channelId eq inputChannelId)
}
