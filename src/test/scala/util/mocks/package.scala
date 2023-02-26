package util

import db.repository._
import zio.mock._

package object mocks {
  @mockable[NotesRepositoryAlg]
  object NotesRepositoryMock

  @mockable[TagsRepositoryAlg]
  object TagsRepositoryMock

  @mockable[NotesTagsRepositoryAlg]
  object NotesTagsRepositoryMock
}
