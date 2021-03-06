package doobie.example

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.syntax.monad._
import scalaz.concurrent.Task

import doobie.imports._

import java.sql.Connection

import org.h2.jdbcx.JdbcConnectionPool

object ConnectionPool {

  // H2's connection pool wrapped in an effectful target monad. Note that construction isn't RT so
  // we make the smart constructor an effect as well. In real life we would probably add operations
  // to inspect and configure the pool.

  final class JdbcConnectionPoolTransactor[M[_]: Monad: Catchable: Capture] private (pool: JdbcConnectionPool) extends Transactor[M] {
    protected def connect: M[Connection] =
      Capture[M].apply(pool.getConnection) 
  }

  object JdbcConnectionPoolTransactor {
    def create[M[_]: Monad: Catchable: Capture](url: String, user: String, pass: String): M[JdbcConnectionPoolTransactor[M]] = 
      Capture[M].apply(new JdbcConnectionPoolTransactor(JdbcConnectionPool.create(url, user, pass)))
  }

  // Test it out on FirstExample's action

  def tmain: Task[Unit] =
    for {
      t <- JdbcConnectionPoolTransactor.create[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      _ <- t.transact(FirstExample.examples)
    } yield ()

  def main(args: Array[String]) =
    tmain.run

}

