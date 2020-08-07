package ai.pulsar.web.services

import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.ql.h2.H2Db
import ai.platon.pulsar.ql.h2.H2DbConfig
import ai.platon.scent.ScentSession
import ai.platon.scent.common.SqlUtils
import ai.platon.scent.common.options.CellType
import ai.platon.scent.context.ScentContexts
import ai.platon.scent.dom.HarvestOptions
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
class ExtractService {

    val sc by lazy { ScentContexts.createSession() }

    final val sessionFactory = ai.platon.scent.ql.h2.H2SessionFactory::class.java.name
    final val dbConfig = H2DbConfig().apply { memory = true; multiThreaded = true }
    final val connection = H2Db(sessionFactory, dbConfig).getRandomConnection()
    final val stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)

    fun load(url: String): WebPage {
        return sc.load(url)
    }

    fun loadDocument(url: String, options: String = ""): FeaturedDocument {
        return sc.loadDocument(url, HarvestOptions.parse(options))
    }

    fun bySql(model: Map<String, Any>): String {
        val sql = model["sql"] as String
        val rs = stat.executeQuery(sql)
        val entities = SqlUtils.getEntitiesFromResultSet(rs)
        val gson = GsonBuilder().serializeNulls().create()
        return gson.toJson(entities)
    }

    fun harvest(model: Map<String, Any>, session: ScentSession): String {
        val targetUrl = model["u"] as String
        val options = model["options"] as HarvestOptions
        val cellType = model["ct"]
        if (cellType !is String) {
            options.cellType = CellType.PLAIN_TEXT
        } else {
            options.cellType = CellType.fromString(cellType)
        }

        val group = runBlocking { session.harvest(targetUrl, options) }
        return session.buildJson(group, options)
    }
}
