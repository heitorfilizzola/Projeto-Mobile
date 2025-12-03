package com.filizzola.projeto_mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.filizzola.projeto_mobile.data.Tarefa
import org.junit.Rule
import org.junit.Test

class TaskPageUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun addTask_whenTitleIsTypedAndSaveClicked_callsOnSave() {
        var savedTitle = ""
        var savedDesc = ""
        var wasSaved = false

        composeTestRule.setContent {
            AddTaskContent(
                onBackClick = {},
                // CORREÇÃO 1: Adicionado o parâmetro 'dueDate' (ignorado aqui com _)
                onSaveTask = { titulo, desc, _ ->
                    savedTitle = titulo
                    savedDesc = desc
                    wasSaved = true
                }
            )
        }

        composeTestRule.onNodeWithText("Salvar Tarefa").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Título da Tarefa").performTextInput("Comprar Café")
        composeTestRule.onNodeWithText("Descrição (Opcional)").performTextInput("Marca X")

        composeTestRule.onNodeWithText("Salvar Tarefa").assertIsEnabled()
        composeTestRule.onNodeWithText("Salvar Tarefa").performClick()

        assert(wasSaved)
        assert(savedTitle == "Comprar Café")
        assert(savedDesc == "Marca X")
    }

    @Test
    fun taskList_displaysListOfTasks() {
        val dummyTasks = listOf(
            Tarefa(id = "1", titulo = "Tarefa Teste 1", desc = "Desc 1", status = "A fazer", userId = "user1"),
            Tarefa(id = "2", titulo = "Tarefa Teste 2", desc = "Desc 2", status = "A fazer", userId = "user1")
        )

        composeTestRule.setContent {
            TaskListContent(
                tasks = dummyTasks,
                // CORREÇÃO 2: Adicionados parâmetros obrigatórios novos
                userId = "user_teste",
                user = null, // Pode passar null para testes visuais simples
                syncConsent = false,
                onConsentChange = {},
                onLogoutClick = {},
                onAddTaskClick = {},
                onEditTaskClick = {},
                onDeleteTask = {},
                onStatusChange = {},
                onSyncClick = {},
                onConfirmPasswordChange = {}
            )
        }

        // Verifica se os textos estão visíveis na tela
        composeTestRule.onNodeWithText("Tarefa Teste 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tarefa Teste 2").assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("A fazer")
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun taskList_whenAddClicked_callsCallback() {
        var addClicked = false

        composeTestRule.setContent {
            TaskListContent(
                tasks = emptyList(),
                // CORREÇÃO 3: Adicionados parâmetros obrigatórios novos
                userId = "user_teste",
                user = null,
                syncConsent = false,
                onConsentChange = {},
                onLogoutClick = {},
                onAddTaskClick = { addClicked = true },
                onEditTaskClick = {},
                onDeleteTask = {},
                onStatusChange = {},
                onSyncClick = {},
                onConfirmPasswordChange = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Criar").performClick()

        assert(addClicked)
    }

    @Test
    fun editTask_loadsExistingData_andSavesChanges() {
        val existingTask = Tarefa(
            id = "123",
            titulo = "Título Antigo",
            desc = "Desc Antiga",
            status = "A fazer",
            userId = "u1"
        )

        var newTitle = ""
        var wasSaved = false

        composeTestRule.setContent {
            EditTaskContent(
                taskToEdit = existingTask,
                onBackClick = {},
                onSaveClick = { t, d, _ ->
                    newTitle = t
                    wasSaved = true
                }
            )
        }

        composeTestRule.onNodeWithText("Título Antigo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Desc Antiga").assertIsDisplayed()

        composeTestRule.onNodeWithText("Título Antigo")
            .performTextClearance()

        composeTestRule.onNodeWithText("Título da Tarefa")
            .performTextInput("Título Novo")

        composeTestRule.onNodeWithText("Salvar Alterações").performClick()

        assert(wasSaved)
        assert(newTitle == "Título Novo")
    }

    @Test
    fun taskItem_expandsOnTap() {
        val task = Tarefa(
            id = "1",
            titulo = "Clique em mim",
            desc = "Sou uma descrição escondida",
            status = "A fazer",
            userId = "u1"
        )

        composeTestRule.setContent {
            TaskItem(
                tarefa = task,
                isToDoList = true,
                onEditClick = {},
                onDeleteTask = {},
                onStatusChange = {}
            )
        }

        composeTestRule.onNodeWithText("Sou uma descrição escondida").assertDoesNotExist()

        composeTestRule.onNodeWithText("Clique em mim").performClick()

        composeTestRule.onNodeWithText("Sou uma descrição escondida").assertIsDisplayed()
    }
}