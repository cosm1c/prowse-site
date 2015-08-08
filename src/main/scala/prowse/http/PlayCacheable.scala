package prowse.http

import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.Writeable
import play.api.mvc.Results._
import play.api.mvc._
import prowse.http.Cacheable._
import prowse.service.{TimeOps, TimeService}

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.language.higherKinds

trait CacheableActionBuilder[+R[_]] extends ActionFunction[Request, R] {
  self =>

  import prowse.http.PlayCacheable._

  def apply[C: Writeable](block: => Future[Option[CacheableContent[C]]])(implicit timeService: TimeService): Action[AnyContent] = apply(_ => block)

  def apply[C: Writeable](block: R[AnyContent] => Future[Option[CacheableContent[C]]])(implicit timeService: TimeService): Action[AnyContent] = apply(BodyParsers.parse.default)(block)

  def apply[A, C: Writeable](bodyParser: BodyParser[A])(block: R[A] => Future[Option[CacheableContent[C]]])(implicit timeService: TimeService): Action[A] =
    composeAction(new Action[A] {
      def parser = composeParser(bodyParser)

      private def procResponse(request: Request[_], futureMaybeContent: Future[Option[CacheableContent[C]]]): Future[Result] = {
        futureMaybeContent.map(maybeContent =>
          timeService.dateHeader(processConditional(request, maybeContent))
        )(CacheableActionBuilder.this.executionContext)
      }

      def apply(request: Request[A]) = try {
        invokeBlock(request, block.andThen(procResponse(request, _)))
      } catch {
        // NotImplementedError is not caught by NonFatal, wrap it
        case e: NotImplementedError => throw new RuntimeException(e)
        // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
        case e: LinkageError => throw new RuntimeException(e)
      }

      override def executionContext = CacheableActionBuilder.this.executionContext
    })

  protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = bodyParser

  protected def composeAction[A](action: Action[A]): Action[A] = action

  override def andThen[Q[_]](other: ActionFunction[R, Q]): ActionBuilder[Q] = new ActionBuilder[Q] {
    def invokeBlock[A](request: Request[A], block: Q[A] => Future[Result]) =
      self.invokeBlock[A](request, other.invokeBlock[A](_, block))

    override protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = self.composeParser(bodyParser)

    override protected def composeAction[A](action: Action[A]): Action[A] = self.composeAction(action)
  }
}

object CacheableAction extends CacheableActionBuilder[Request] {
  private val logger = Logger(CacheableAction.getClass)

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}

object PlayCacheable extends TimeOps {

  implicit val conditionalQueryRequest = new ConditionalQuery[Request[_]] {

    override def precondition(request: Request[_]): Option[QueryCondition] = parsePrecondition(request)

    override def cacheValidation(request: Request[_]): Option[QueryCondition] = parseCacheValidation(request)

    private val parsePrecondition = parseConditionalQueryHeaders(IF_MATCH, IF_UNMODIFIED_SINCE)(_)

    private val parseCacheValidation = parseConditionalQueryHeaders(IF_NONE_MATCH, IF_MODIFIED_SINCE)(_)

    private def parseConditionalQueryHeaders(eTagHeaderName: String, lastModifiedHeaderName: String)(request: Request[_]) = {
      val headers: Headers = request.headers

      def optionalETag: Option[QueryCondition] =
        headers.get(eTagHeaderName)
          .map(ETag.parseETagsHeader)
          .map(Left(_))

      def optionalLastModified: Option[QueryCondition] =
        headers.get(lastModifiedHeaderName)
          .flatMap(parseLastModifiedHeaderValue)
          .map(Right(_))

      optionalETag.orElse(optionalLastModified)
    }

    private def parseLastModifiedHeaderValue(value: String): Option[LastModified] = parseDate(value).toOption
  }

  def processConditional[C: Writeable](request: Request[_], maybeContent: Option[CacheableContent[C]]): Result = {
    def resultIsCacheableContent(content: CacheableContent[C]): Result = {
      val result: Result =
        if (!preconditionCheck(request, content))
          PreconditionFailed
        else if (cacheValidationCheck(request, content))
          NotModified
        else
          Ok(content.content)

      result.withHeaders(cachingHeadersFor(content): _*)
    }

    def resultIsMissing: Result = {
      if (preconditionCheckWhenMissing(request))
        PreconditionFailed
      else
        NotFound
    }

    def cachingHeadersFor(content: CacheableContent[_]) =
      Seq(
        ETAG -> content.eTag.toString,
        LAST_MODIFIED -> formatHttpDate(content.lastModified.toInstant),
        // TODO: Cache-Control header should be dynamic (allow to change with server load)
        // minimum retention of 1 second as that is resolution of HTTP dates
        CACHE_CONTROL -> "max-age=1,s-maxage=1"
      )

    maybeContent match {
      case Some(content) => resultIsCacheableContent(content)
      case None => resultIsMissing
    }
  }


}
