package nl.greaper.bnplanner.datasource

import com.mongodb.client.MongoDatabase
import nl.greaper.bnplanner.BaseTest
import nl.greaper.bnplanner.model.Gamemode
import nl.greaper.bnplanner.model.Role
import nl.greaper.bnplanner.model.User
import nl.greaper.bnplanner.model.UserGamemode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor

@ContextConfiguration
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UserDataSourceTest(database: MongoDatabase) : BaseTest() {
    private val dataSource = UserDataSource(database)

    companion object {
        val TEST_USER = User(
            osuId = "1",
            username = "TEST_USER",
            gamemodes = listOf(UserGamemode(Gamemode.osu, Role.Nominator))
        )
        val TEST_USER_2 = TEST_USER.copy(osuId = "2")
        val TEST_USER_3 = User(
            osuId = "3",
            username = "TEST_USER_3",
            gamemodes = listOf(UserGamemode(Gamemode.osu, Role.Nominator), UserGamemode(Gamemode.fruits, Role.Nominator))
        )
    }

    @Test
    fun `Should add user to in memory caches correctly`() {
        dataSource.addInMemoryUser(TEST_USER)

        val expectedById = mapOf("1" to TEST_USER)
        val expectedByGamemode = createUsersByGamemode(osu = setOf("1"))

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    @Test
    fun `Should add multiple user to in memory caches correctly with same gamemode`() {
        dataSource.addInMemoryUser(TEST_USER)
        dataSource.addInMemoryUser(TEST_USER_2)

        val expectedById = mapOf(
            "1" to TEST_USER,
            "2" to TEST_USER_2
        )
        val expectedByGamemode = createUsersByGamemode(osu = setOf("1", "2"))

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    @Test
    fun `Should add user to in memory caches correctly with multiple gamemodes`() {
        dataSource.addInMemoryUser(TEST_USER_3)

        val expectedById = mapOf("3" to TEST_USER_3)
        val expectedByGamemode = createUsersByGamemode(osu = setOf("3"), fruits = setOf("3"))

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    @Test
    fun `Should update user in memory caches correctly when adding new gamemode`() {
        dataSource.addInMemoryUser(TEST_USER)

        val updatedUser = TEST_USER.copy(gamemodes = TEST_USER.gamemodes + UserGamemode(Gamemode.mania, Role.NominationAssessment))
        dataSource.addInMemoryUser(updatedUser)

        val expectedById = mapOf("1" to updatedUser)
        val expectedByGamemode = createUsersByGamemode(osu = setOf("1"), mania = setOf("1"))

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    @Test
    fun `Should update user in memory caches correctly when removing gamemode`() {
        dataSource.addInMemoryUser(TEST_USER_3)

        val updatedUser = TEST_USER_3.copy(gamemodes = listOf(UserGamemode(Gamemode.osu, Role.Probation)))
        dataSource.addInMemoryUser(updatedUser)

        val expectedById = mapOf("3" to updatedUser)
        val expectedByGamemode = createUsersByGamemode(osu = setOf("3"))

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    @Test
    fun `Should remove user from in memory caches correctly`() {
        dataSource.addInMemoryUser(TEST_USER)
        dataSource.removeInMemoryUser(TEST_USER)

        val expectedById = emptyMap<String, User>()
        val expectedByGamemode = createUsersByGamemode()

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    @Test
    fun `Should not throw exception when removing user from in memory caches when never in cache`() {
        dataSource.removeInMemoryUser(TEST_USER)

        val expectedById = emptyMap<String, User>()
        val expectedByGamemode = createUsersByGamemode()

        assertEquals(expectedById, dataSource.usersByOsuId)
        assertEquals(expectedByGamemode, dataSource.usersByGamemode)
    }

    private fun createUsersByGamemode(
        osu: Set<String> = emptySet(),
        taiko: Set<String> = emptySet(),
        fruits: Set<String> = emptySet(),
        mania: Set<String> = emptySet()
    ): Map<Gamemode, Set<String>> {
        return mapOf(
            Gamemode.osu to osu,
            Gamemode.taiko to taiko,
            Gamemode.fruits to fruits,
            Gamemode.mania to mania
        )
    }
}
