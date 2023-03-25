package solutions.shitops.queries

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatest.TryValues
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

abstract class BaseSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with EitherValues
    with OptionValues
    with TryValues
    with Matchers {}
