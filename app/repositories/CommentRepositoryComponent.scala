package repositories

import scala.slick.session.Session
import models.database._
import models.database.Comment
import models.database.CommentToInsert

/**
 * Provides persistence for article comments
 */
trait CommentRepositoryComponent {
  val commentRepository: CommentRepository

  trait CommentRepository {

    def getByArticle(id: Int)(implicit session: Session): List[Comment]

    def insert(comment: CommentToInsert)(implicit session: Session): Int

    def update(comment: CommentToUpdate)(implicit session: Session): Boolean

    def delete(id: Int)(implicit session: Session): Boolean
  }
}

trait CommentRepositoryComponentImpl extends CommentRepositoryComponent {
  this: CommentsSchemaComponent with Profile =>

  val commentRepository = new SlickCommentRepository

  class SlickCommentRepository extends CommentRepository {

    import profile.simple._

    def getByArticle(id: Int)(implicit session: Session) = {
      Query(Comments)
        .filter(_.articleId == id)
        .sortBy(_.createdAt.desc)
        .list
    }

    def insert(comment: CommentToInsert)(implicit session: Session) = {
        Comments.forInsert.insert(comment)
    }

    def update(comment: CommentToUpdate)(implicit session: Session) = {
     Comments
        .filter(_.id === comment.id)
        .map(_.forUpdate)
        .update(comment) > 0
    }

    def delete(id: Int)(implicit session: Session) = {
      Comments.where(_.id === id).delete > 0
    }
  }

}
