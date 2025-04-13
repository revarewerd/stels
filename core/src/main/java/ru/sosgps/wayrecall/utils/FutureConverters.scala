package ru.sosgps.wayrecall.utils

import java.util.concurrent.Executor

import com.google.common.util.concurrent.{FutureCallback, MoreExecutors}

import scala.concurrent.{ExecutionContext$, ExecutionContext, Future}
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.asJavaCollection
import scala.util.{Failure, Success}

/**
 * Created by nickl on 29.10.14.
 */
object FutureConverters {

  def fromExecutor(e: Executor)=  ExecutionContext.fromExecutor(e)

  def sequence[T](futures: java.util.Collection[Future[T]], exctx: ExecutionContext):Future[java.util.Collection[T]] = {
       implicit val s = exctx
       Future.sequence(futures:Iterable[Future[T]]).map(c => c:java.util.Collection[T])
  }

  def addCallBack[T](f: Future[T], cb: FutureCallback[T]) = {
    import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
    f.onComplete{
      case Success(r) => cb.onSuccess(r)
      case Failure(e) => cb.onFailure(e)
    }
  }

}


