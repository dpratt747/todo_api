package util

import db.persistence._
import db.repository._
import program._
import zio.mock._

package object mocks {
  @mockable[NotesRepositoryAlg]
  object NotesRepositoryMock

  @mockable[TagsRepositoryAlg]
  object TagsRepositoryMock

  @mockable[NotesTagsRepositoryAlg]
  object NotesTagsRepositoryMock

  @mockable[NotesTagsPersistenceAlg]
  object NotesTagsPersistenceMock

  @mockable[CreateNoteProgramAlg]
    object CreateNoteProgramMock
}
