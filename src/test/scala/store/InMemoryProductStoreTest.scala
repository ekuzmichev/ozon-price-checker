package ru.ekuzmichev
package store

import common.ProductId
import store.InMemoryProductStore.ProductState
import store.ProductStore.{Product, SourceId, SourceState}

import zio.test.*
import zio.test.Assertion.*
import zio.{Ref, Scope, UIO}

object InMemoryProductStoreTest extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("InMemoryProductStore")(
      test("preInitialize should fill state with initial data") {
        import Fixtures.sourceStatesBySortId
        for
          productStore              <- makeProductStore()
          stateBeforeInitialization <- productStore.readAll()
          _                         <- productStore.preInitialize(sourceStatesBySortId)
          stateAfterInitialization  <- productStore.readAll()
        yield assertTrue(stateBeforeInitialization.isEmpty) &&
          assert(stateAfterInitialization)(hasSameElements(sourceStatesBySortId))
      },
      test("checkInitialized should return true if sourceId exists and false otherwise") {
        import Fixtures.{bobSourceId, fritzSourceId, pamelaSourceId}
        for
          productStore      <- makePreInitializedProductStore()
          bobInitialized    <- productStore.checkInitialized(bobSourceId)
          fritzInitialized  <- productStore.checkInitialized(fritzSourceId)
          pamelaInitialized <- productStore.checkInitialized(pamelaSourceId)
        yield assertTrue(bobInitialized, fritzInitialized, !pamelaInitialized)
      },
      test("emptyState should remove all products for sourceId") {
        import Fixtures.bobSourceId
        for
          productStore        <- makePreInitializedProductStore()
          _                   <- productStore.emptyState(bobSourceId)
          stateAfterEmptiness <- productStore.readAll()
        yield assertTrue(stateAfterEmptiness(bobSourceId).products.isEmpty)
      },
      test("clearState should remove whole state for sourceId") {
        import Fixtures.bobSourceId
        for
          productStore        <- makePreInitializedProductStore()
          _                   <- productStore.clearState(bobSourceId)
          stateAfterClearance <- productStore.readAll()
        yield assertTrue(!stateAfterClearance.contains(bobSourceId))
      },
      test("readSourceState should read state for sourceId") {
        import Fixtures.{bobSourceId, bobSourceState}
        for
          productStore       <- makePreInitializedProductStore()
          readBobSourceState <- productStore.readSourceState(bobSourceId)
        yield assertTrue(readBobSourceState.get == bobSourceState)
      },
      test("addProduct should add product to product list of sourceId if sourceId exists") {
        import Fixtures.{bobSourceId, pamelaSourceId}
        for
          productStore <- makePreInitializedProductStore()
          newProduct = Product("blue-water", 1500)
          bobProductAdded    <- productStore.addProduct(bobSourceId, newProduct)
          readBobSourceState <- productStore.readSourceState(bobSourceId)
          pamelaProductAdded <- productStore.addProduct(pamelaSourceId, newProduct)
        yield assertTrue(bobProductAdded, readBobSourceState.get.products.contains(newProduct), !pamelaProductAdded)
      },
      test("checkHasProductId should return true if product with id exists and false otherwise") {
        import Fixtures.{bobSourceId, redKettleProductId, whiteLampProductId}
        for
          productStore        <- makePreInitializedProductStore()
          hasRedKettleProduct <- productStore.checkHasProductId(bobSourceId, redKettleProductId)
          hasWhiteLampProduct <- productStore.checkHasProductId(bobSourceId, whiteLampProductId)
        yield assertTrue(hasRedKettleProduct, !hasWhiteLampProduct)
      },
      test("removeProduct by productId should remove product from product list of sourceId if sourceId exists") {
        import Fixtures.{bobSourceId, pamelaSourceId, redKettleProductId}
        for
          productStore         <- makePreInitializedProductStore()
          bobProductRemoved    <- productStore.removeProduct(bobSourceId, redKettleProductId)
          readBobSourceState   <- productStore.readSourceState(bobSourceId)
          pamelaProductRemoved <- productStore.removeProduct(pamelaSourceId, redKettleProductId)
        yield assertTrue(
          bobProductRemoved,
          !readBobSourceState.get.products.exists(_.id == redKettleProductId),
          !pamelaProductRemoved
        )
      },
      test("removeProduct by productIndex should remove product from product list of sourceId if sourceId exists") {
        import Fixtures.{bobSourceId, pamelaSourceId, redKettleProductId}
        for
          productStore         <- makePreInitializedProductStore()
          bobProductRemoved    <- productStore.removeProduct(bobSourceId, 0)
          readBobSourceState   <- productStore.readSourceState(bobSourceId)
          pamelaProductRemoved <- productStore.removeProduct(pamelaSourceId, 0)
        yield assertTrue(
          bobProductRemoved,
          !readBobSourceState.get.products.exists(_.id == redKettleProductId),
          !pamelaProductRemoved
        )
      }
    )

  private def makePreInitializedProductStore(): UIO[InMemoryProductStore] =
    import Fixtures.sourceStatesBySortId
    makeProductStore().tap(_.preInitialize(sourceStatesBySortId).ignore)

  private def makeProductStore(): UIO[InMemoryProductStore] =
    for productStateRef <- Ref.make(ProductState.empty)
    yield new InMemoryProductStore(productStateRef)

  private object Fixtures:
    val bobSourceId: SourceId    = SourceId("bob", "123")
    val fritzSourceId: SourceId  = SourceId("fritz", "456")
    val pamelaSourceId: SourceId = SourceId("pamela", "789")

    val redKettleProductId: ProductId    = "red-kettle"
    val brownPenProductId: ProductId     = "brown-pen"
    val yellowPillowProductId: ProductId = "yellow-pillow"
    val darkToyProductId: ProductId      = "dark-toy"
    val greenGrassProductId: ProductId   = "green-grass"
    val whiteLampProductId: ProductId    = "white-lamp"

    val bobSourceState: SourceState = SourceState(
      products = Seq(
        Product(redKettleProductId, 1000),
        Product(brownPenProductId, 300)
      ),
      maybeProductCandidate = None
    )

    val fritzSourceState: SourceState = SourceState(
      products = Seq(
        Product(yellowPillowProductId, 2000),
        Product(darkToyProductId, 300),
        Product(greenGrassProductId, 100)
      ),
      maybeProductCandidate = None
    )

    val sourceStatesBySortId: Map[SourceId, SourceState] = Map(
      bobSourceId   -> bobSourceState,
      fritzSourceId -> fritzSourceState
    )
