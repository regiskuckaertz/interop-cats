package zio.stream.interop

import cats.Eq
import cats.effect.laws.util.TestContext
import cats.implicits._
import org.scalacheck.{ Arbitrary, Cogen, Gen }
import zio.interop.catz.taskEffectInstance
import zio.interop.catzSpecBase
import zio.stream._
import zio.ZIO

private[interop] trait catzSpecZStreamBase
    extends catzSpecBase
    with catzSpecZStreamBaseLowPriority
    with GenStreamInteropCats {

  implicit def zstreamEqStream[E: Eq, A: Eq](implicit tc: TestContext): Eq[Stream[E, A]] = Eq.by(_.either)

  implicit def zstreamEqUStream[A: Eq](implicit tc: TestContext): Eq[Stream[Nothing, A]] =
    Eq.by(ustream => taskEffectInstance.toIO(ustream.runCollect.sandbox.either))

  implicit def zstreamArbitrary[R: Cogen, E: Arbitrary: Cogen, A: Arbitrary: Cogen]: Arbitrary[ZStream[R, E, A]] =
    Arbitrary(Arbitrary.arbitrary[R => Stream[E, A]].map(ZStream.fromEffect(ZIO.environment[R]).flatMap(_)))

  implicit def streamArbitrary[E: Arbitrary: Cogen, A: Arbitrary: Cogen]: Arbitrary[Stream[E, A]] =
    Arbitrary(Gen.oneOf(genStream[E, A], genLikeTrans(genStream[E, A]), genIdentityTrans(genStream[E, A])))
}

private[interop] trait catzSpecZStreamBaseLowPriority { self: catzSpecZStreamBase =>

  implicit def zstreamEq[R: Arbitrary, E, A: Eq](implicit tc: TestContext): Eq[ZStream[R, E, A]] = {
    def run(r: R, zstream: ZStream[R, E, A]) = taskEffectInstance.toIO(zstream.runCollect.provide(r).sandbox.either)
    Eq.instance(
      (stream1, stream2) =>
        Arbitrary.arbitrary[R].sample.fold(false)(r => catsSyntaxEq(run(r, stream1)) eqv run(r, stream2))
    )
  }

}
