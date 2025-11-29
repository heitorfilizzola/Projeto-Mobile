package com.filizzola.projeto_mobile.viewmodel

import android.app.Application
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: TaskViewModel
    private val application = mockk<Application>(relaxed = true)

    // User de teste
    private val userId = "user123"
    private val testUser = User(
        id = userId,
        username = "Test",
        email = "test@test.com",
        uTaskList = arrayListOf()
    )

    @Before
    fun setup() {
        mockkObject(UserRepository)
        mockkConstructor(LoginManager::class)
        coEvery { anyConstructed<LoginManager>().clearLoggedUser() } just Runs

        UserRepository.allUsers.clear()
        UserRepository.allUsers.add(testUser)

        viewModel = TaskViewModel(application)
    }

    @Test
    fun `loadTasks deve carregar tarefas do usuario`() = runTest {
        // Arrange
        val task = Tarefa(id = "t1", titulo = "Test Task", status = "A fazer", userId = userId)
        testUser.uTaskList.add(task)

        // Act
        viewModel.loadTasks(userId)

        // Assert
        assertEquals(1, viewModel.tasks.value.size)
        assertEquals("Test Task", viewModel.tasks.value[0].titulo)
        assertEquals(testUser, viewModel.user.value)
    }

    @Test
    fun `addTask deve chamar UserRepository e recarregar tarefas`() = runTest {
        // Arrange
        val newTask = Tarefa(titulo = "Nova Tarefa", status = "A fazer", userId = userId)

        // Mock do método addTaskToUser para não tentar chamar o Supabase real
        coEvery { UserRepository.addTaskToUser(userId, any()) } answers {
            // Simula o comportamento de adicionar à lista na memória
            testUser.uTaskList.add(arg(1))
        }

        // Act
        viewModel.addTask(userId, newTask)

        // Assert
        coVerify { UserRepository.addTaskToUser(userId, any()) }
        assertEquals(1, viewModel.tasks.value.size)
        assertEquals("Nova Tarefa", viewModel.tasks.value[0].titulo)
    }

    @Test
    fun `changeTaskStatus deve alternar o status da tarefa`() = runTest {
        // Arrange
        val task = Tarefa(id = "t1", titulo = "Task", status = "A fazer", userId = userId)
        testUser.uTaskList.add(task)
        viewModel.loadTasks(userId) // Carrega estado inicial

        // Mock do update
        coEvery { UserRepository.updateTaskForUser(userId, any()) } answers {
            val updated = arg<Tarefa>(1)
            val index = testUser.uTaskList.indexOfFirst { it.id == updated.id }
            testUser.uTaskList[index] = updated
        }

        // Act
        viewModel.changeTaskStatus(userId, task)

        // Assert
        val currentTasks = viewModel.tasks.value
        assertEquals("Feito", currentTasks[0].status)
    }
}