package services

import org.specs2.mutable.Specification
import models.database._
import utils.DateImplicits._
import org.specs2.time.NoTimeConversions
import com.github.nscala_time.time.Imports._
import org.specs2.mock.Mockito
import repositories.ArticlesRepositoryComponent
import org.mockito.Matchers
import models.ArticleModels.{ArticleDetailsModel, Article, ArticleListModel}
import util.{TimeFridge, MockSession}
import models.Page
import org.specs2.specification.BeforeExample
import scalaz._
import Scalaz._
import validators.Validator
import util.ScalazValidationTestUtils._
import org.specs2.scalaz.ValidationMatchers


class ArticlesServiceSpec extends Specification with NoTimeConversions with Mockito
  with BeforeExample with ValidationMatchers with MockSession {

  object service extends ArticlesServiceComponentImpl
      with ArticlesRepositoryComponent
      with TagsServiceComponent with MockSessionProvider {
    override val articlesRepository = mock[ArticlesRepository]
    override val tagsService = mock[TagsService]
    override val articleValidator = mock[Validator[Article]]
  }

  import service._

  def before = {
    org.mockito.Mockito.reset(tagsService)
    org.mockito.Mockito.reset(articlesRepository)
    org.mockito.Mockito.reset(articleValidator)
    org.mockito.Mockito.reset(session)
  }

  val dbRecord = {
    val article = ArticleRecord(Some(1), "", "", DateTime.now, DateTime.now, "", 1)
    val user = UserRecord(Some(1), "")
    val tags = List("tag1", "tag2")
    (article, user, tags)
  }

  "get article" should {
    val article = Some(dbRecord)

    "return model" in {
      articlesRepository.get(anyInt)(Matchers.eq(session)) returns article

      val model = articlesService.get(1)

      model.map(_.id) must beSome(1)
    }

    "have correct author" in {
      articlesRepository.get(anyInt)(Matchers.eq(session)) returns article

      val model = articlesService.get(1)

      model.map(_.author.id) must beSome(1)
    }

    "have correct tags" in {
      articlesRepository.get(anyInt)(Matchers.eq(session)) returns article

      val model = articlesService.get(1)

      model.map(_.tags).get must containTheSameElementsAs(dbRecord._3)
    }
  }

  "article removal" should {
    "remove article" in {
      articlesService.removeArticle(1)

      there was one(articlesRepository).remove(1)(session)
    }
  }

  "paginated articles list" should {
    "request second page with correct parameters" in {
      articlesRepository.getList(anyInt, anyInt)(Matchers.eq(session)) returns List()

      articlesService.getPage(2)

      there was one(articlesRepository).getList(6, 3)(session)
    }

    "contain list models" in {
      articlesRepository.getList(anyInt, anyInt)(Matchers.eq(session)) returns List(dbRecord)

      val model: Page[ArticleListModel] = articlesService.getPage(1)

      model.list(0).id must_== 1
      model.list(0).author.id must_== 1
    }

    "contain current page" in {
      articlesRepository.getList(anyInt, anyInt)(Matchers.eq(session)) returns List(dbRecord)

      val model = articlesService.getPage(1)

      model.currentPage must_== 1
    }

    "contain total pages count" in {
      val count = 5
      articlesRepository.getList(anyInt, anyInt)(Matchers.eq(session)) returns List(dbRecord)
      articlesRepository.count()(Matchers.eq(session)) returns count

      val model = articlesService.getPage(1)

      model.totalPages must_== 2
    }
  }

  "creating new article" should {
    val article = Article(None, "", "", List())

    "insert new article" in {
      TimeFridge.withFrozenTime() { dt =>
        val record = ArticleToInsert("", "", dt, dt, "", 1)
        articlesRepository.insert(any[ArticleToInsert])(Matchers.eq(session)) returns 1
        articleValidator.validate(any[Article]) returns article.successNel
        tagsService.createTagsForArticle(anyInt, any[Seq[String]]) returns Success(Unit)

        articlesService.createArticle(article)

        there was one(articlesRepository).insert(record)(session)
      }
    }

    "return model" in {
      articlesRepository.insert(any[ArticleToInsert])(Matchers.eq(session)) returns 1
      articleValidator.validate(any[Article]) returns article.successNel
      tagsService.createTagsForArticle(anyInt, any[Seq[String]]) returns Success(Unit)

      val model: ArticleDetailsModel = articlesService.createArticle(article).get

      model.id must_== 1
    }

    "create tags" in {
      val tags = List("tag1", "tag2")
      val articleId = 1
      articlesRepository.insert(any[ArticleToInsert])(Matchers.eq(session)) returns articleId
      articleValidator.validate(any[Article]) returns article.successNel
      tagsService.createTagsForArticle(anyInt, any[Seq[String]]) returns Success(Unit)

      articlesService.createArticle(article.copy(tags = tags))

      there was one(tagsService).createTagsForArticle(articleId, tags)
    }

    "not create article when validation failed" in {
      articleValidator.validate(any[Article]) returns "".failNel
      tagsService.createTagsForArticle(anyInt, any[Seq[String]]) returns Success(Unit)

      articlesService.createArticle(article)

      there was noMoreCallsTo(articlesRepository, tagsService)
    }

    "rollback transaction when tags creation failed" in {
      articleValidator.validate(any[Article]) returns article.successNel
      tagsService.createTagsForArticle(anyInt, any[Seq[String]]) returns "".failNel

      articlesService.createArticle(article) must beFailing

      there was one(session).rollback()
    }
  }

  "article update" should {
    val article = Article(None, "", "", List())

    "update existing article" in {
      articleValidator.validate(any[Article]) returns article.successNel
      articlesService.updateArticle(Article(Some(1), "title", "content", List("tag")))

      there was one(articlesRepository).update(
        Matchers.eq(1), any[ArticleToUpdate])(Matchers.eq(session))
    }

    "update modification time" in {
      articleValidator.validate(any[Article]) returns article.successNel
      TimeFridge.withFrozenTime() { now =>
        articlesService.updateArticle(Article(Some(1), "", "", List("")))

        there was one(articlesRepository).update(1, ArticleToUpdate("", "", now, ""))(session) //TODO: match only modification time
      }
    }
  }
}
