# Ktor Starter

Basic app setup with Ktor, OpenApi, Postgres, Exposed, and Flyway.

## Using this Starter

The preferred approach is to fork or clone this repository into your own github space,
then to locally (on your machine) add this starter repo as a remote so you can fetch
security and functional updates to the starter in the future.

```shell
# TODO - actually verify these steps
# Add this repo as a remote named `base`
git remote add base <URL to this repo>
# To sync your local repo with changes from this starter
# First: fetch the main branch from base remote 
git fetch base main
# Second: add those changes to a new branch, here called `sync-with-base`
git checkout -b sync-with-base base/main
# Third: Fix conflicts and add all the changes
git add .
# Forth: Commit the changes
git commit -m "syncing with base project version x.y.z"
# Fifth: Merge back to local main
git checkout main && git merge sync-with-base
```

## Running the project

**Postgres**

Startup the local postgres database defined in the `docker-compose.yml` file with

```shell
# From project root directory
docker-compose up
```

**Ktor Server**

Then run the project as usual from IntelliJ, either by running the `main` function 
in `app` module, or the `runShadow` gradle task.

## Creating and migrating data

**Introduction**

This project is setup with JetBrains Exposed library and Flyway migration. Tables 
are defined as objects using the `exposed` library. When connecting to postgres during 
app startup in the `connectToPostgres` function, Flyway will first attempt any pending 
migrations from the `db.migration` folder in `data` modules `resources`. Then Exposed
will check that there are no outstanding statements required to align our code's table 
definitions with whats in the database. If any exceptions are found, it will print out
the migration scripts you need to add and then stop the server. It's up to you to 
understand how to add these missing SQL statements to Flyway migration (for now, maybe 
someday we can get smarted about it).

What this means practically is that you can define or modify your table in the code, 
run the app, then check the failure message for the SQL code you need to add in
order to inform Flyway of the needed updates. It's important to *always* manage
this through Flyway as it keeps track of all changes / creations to enable
smooth rollbacks or updates to the database. 

**Creating Data**

There are three main parts when adding new types into the data module. These parts are 
* The Domain Model - a standard Kotlin data class
* The Exposed Table - a table definition using Exposed classes and methods, typically `IntIdTable`
* The Exposed Entity DAO - an Exposed entity (typically `IntEntity`) which implements 1 
or more interfaces from `Resource<T>`. Should also include a method of adapting database
query results into the Domain Model.

**Example**

```kotlin
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
// Domain Model
data class User(val id: Int, val name: String, val email: String)
// Exposed Table
object UsersTable : Table() {
    val id = integer("id").autoIncrement().primaryKey() // Defines the primary key
    val name = varchar("name", length = 255) // Defines a VARCHAR column
    val email = varchar("email", length = 255) // Defines another VARCHAR column
}
// Exposed Entity DAO
class UserEntity(id: EntityID<Int>) : IntEntity(id), Resource.Create<User>, Resource.Read<User> {
    companion object : IntEntityClass<UserEntity>(UsersTable)

    var name by UsersTable.name
    var email by UsersTable.email

    fun toModel(): User = User(id.value, name, email)

    // Resource.Create<User> override
    override fun create(resource: User): User {
        val userEntity = transaction {
            UserEntity.new {
                this.name = resource.name
                this.email = resource.email
            }
        }
        return userEntity.toModel()
    }
    // Resource.Read<User> override for single user
    override fun getById(id: Int): User {
        val userEntity = transaction {
            UserEntity.findById(userId)
        }
        return userEntity?.toModel() ?: throw NotFoundException()
    }
    // Resource.Read<User> override for paginated collection of Users
    override fun getAll(limit: Int, page: Int): List<User> {
        val offset = (page - 1) * limit
        return transaction {
            UserEntity.all()
                .limit(limit, offset)
                .map { it.toModel() }
        }
    }
}
```

**Flyway Notes**

Flyway is particular about capitalization. Make sure your prefixes are `R__` for 
root files (initial table definitions) and `V1__`, `V2__`, ... `Vx__` for updates
(usually `ALTER` statements). Also make sure the naming is descriptive.
* Bad Example - `V2__Update_Users.sql`
* Good Example - `V2__Add_Profile_Picture_To_Users.sql`

It's important to think of any set of flyway migrations as a logical unit of work. If 
the alterations are strictly dependant on one another and comprise a single unit of work
on a given portion of functionality, they should go in the same SQL file so they can be 
tracked / updated / rolled back together. If not, separate files should be used for
each alteration or update.