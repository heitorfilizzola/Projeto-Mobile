package com.filizzola.projeto_mobile.viewmodel

import android.app.Application
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.SyncManager
import com.filizzola.projeto_mobile.utils.MainDispatcherRule
import com.filizzola.projeto_mobile.utils.NotificationHelper
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        mockkObject(NotificationHelper) // Mock NotificationHelper

        // Mock construction of Utils classes inside ViewModel
        mockkConstructor(LoginManager::class)
        mockkConstructor(SyncManager::class)

        // Default behaviors
        coEvery { anyConstructed<LoginManager>().clearLoggedUser() } returns Unit
        coEvery { anyConstructed<SyncManager>().saveTaskLocally(any(), any()) } returns 0 // saveTaskLocally returns Int (from Log.d)
        coEvery { NotificationHelper.scheduleNotification(any(), any()) } returns Unit
        coEvery { NotificationHelper.cancelNotification(any(), any()) } returns Unit

        UserRepository.allUsers.clear()
        UserRepository.allUsers.add(testUser)

        viewModel = TaskViewModel(application)
    }

    @Test
    fun `loadTasks deve carregar tarefas do usuario filtrando as deletadas`() = runTest {
        // Arrange
        val activeTask = Tarefa(
            id = "t1", 
            titulo = "Active", 
            status = "A fazer", 
            userId = userId, 
            isDeleted = false
        )
        val deletedTask = Tarefa(
            id = "t2", 
            titulo = "Deleted", 
            status = "A fazer", 
            userId = userId, 
            isDeleted = true
        )
        testUser.uTaskList.clear()
        testUser.uTaskList.add(activeTask)
        testUser.uTaskList.add(deletedTask)

        // Act
        viewModel.loadTasks(userId)

        // Assert
        assertEquals(1, viewModel.tasks.value.size)
        assertEquals("Active", viewModel.tasks.value[0].titulo)
        assertEquals(testUser, viewModel.user.value)
    }

    @Test
    fun `addTask deve chamar UserRepository, agendar notificacao e salvar no disco`() = runTest {
        // Arrange
        val newTask = Tarefa(titulo = "Nova Tarefa", status = "A fazer", userId = userId)

        // Mock do método addTaskToUser
        coEvery { UserRepository.addTaskToUser(userId, any()) } answers {
            testUser.uTaskList.add(arg(1))
        }

        // Act
        viewModel.addTask(userId, newTask)

        // Assert
        coVerify { UserRepository.addTaskToUser(userId, any()) }
        coVerify { NotificationHelper.scheduleNotification(any(), any()) }
        coVerify { anyConstructed<SyncManager>().saveTaskLocally(userId, any()) } // Verifica persistência
        
        assertEquals(1, viewModel.tasks.value.size)
        assertEquals("Nova Tarefa", viewModel.tasks.value[0].titulo)
    }

    @Test
    fun `syncTasks deve chamar syncUserData e retornar sucesso`() = runTest {
        // Arrange
        coEvery { UserRepository.syncUserData(userId) } returns emptyList() // Simula sucesso
        var callbackResult = false

        // Act
        viewModel.syncTasks(userId) { success ->
            callbackResult = success
        }

        // Assert
        coVerify { UserRepository.syncUserData(userId) }
        coVerify { anyConstructed<SyncManager>().saveTaskLocally(userId, any()) }
        assertTrue(callbackResult)
    }

    @Test
    fun `deleteTask deve marcar e salvar no disco`() = runTest {
        // Arrange
        val task = Tarefa(id = "t1", titulo = "To Delete", status = "A fazer", userId = userId)
        testUser.uTaskList.add(task)
        viewModel.loadTasks(userId)

        coEvery { UserRepository.deleteTaskForUser(userId, any()) } answers {
            // Simula comportamento do Repo (Soft delete na RAM)
            val t = testUser.uTaskList.find { it.id == arg(1) }
            // Na implementação real do repo ele altera o objeto, aqui removemos ou simulamos
            // Como o ViewModel recarrega filtrando !isDeleted, vamos remover da lista 'visível' do repo mock
             testUser.uTaskList.removeIf { it.id == arg(1) }
        }

        // Act
        viewModel.deleteTask(userId, "t1")

        // Assert
        coVerify { UserRepository.deleteTaskForUser(userId, "t1") }
        coVerify { NotificationHelper.cancelNotification(any(), "t1") }
        coVerify { anyConstructed<SyncManager>().saveTaskLocally(userId, any()) }
        assertEquals(0, viewModel.tasks.value.size)
    }
}