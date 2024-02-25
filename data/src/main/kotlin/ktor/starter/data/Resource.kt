package ktor.starter.data

sealed interface Resource<T> {
    interface Create<T> : Resource<T> {
        fun create(resource: T) : T
        // TODO - bulk create?
    }
    interface Read<T> : Resource<T> {
        // TODO - page class instead of list
        fun getAll(page: Int, limit: Int) : List<T>
        fun getById(id: Int) : T
    }
    interface Update<T> : Resource<T> {
        fun update(resource: T) : T
        fun transform(transformer: (T) -> T) : T
    }
    interface Delete<T> : Resource<T> {
        fun deleteById(id: String) : T
    }
    interface Crud<T> : Create<T>, Read<T>, Update<T>, Delete<T>
}