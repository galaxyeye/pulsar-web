package ai.pulsar.web

import ai.pulsar.web.services.ExtractService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException

@Controller
class HtmlController(private val repository: ArticleRepository,
                     private val properties: BlogProperties) {

	@Autowired
	lateinit var extractService: ExtractService

	@GetMapping("/page/load")
	fun load(@RequestParam("u") url: String, model: Model): String {
		val document = extractService.loadDocument(url)
		document.absoluteLinks()
		document.stripScripts()
		val html = document.prettyHtml
				.replace("<html", "<div").replace("</html>", "</div>")
				.replace("<body", "<div").replace("</body>", "</div>")

		val article = Article(document.title, document.location, html)
		model["title"] = article.title
		model["article"] = article
		return "page"
	}

	@GetMapping("/")
	fun blog(model: Model): String {
		model["title"] = properties.title
		model["banner"] = properties.banner
		model["articles"] = repository.findAllByOrderByAddedAtDesc().map { it.render() }
		return "blog"
	}

	@GetMapping("/article/{slug}")
	fun article(@PathVariable slug: String, model: Model): String {
		val article = repository.findBySlug(slug)?.render()
				?: throw ResponseStatusException(NOT_FOUND, "This article does not exist")
		model["title"] = article.title
		model["article"] = article
		return "article"
	}

	fun Article.render() = RenderedArticle(
            slug,
            title,
            headline,
            content,
            author,
            addedAt.format()
    )

	data class RenderedArticle(
            val slug: String,
            val title: String,
            val headline: String,
            val content: String,
            val author: User,
            val addedAt: String)

}
