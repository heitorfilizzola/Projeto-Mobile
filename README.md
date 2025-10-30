# NoteSync

`NoteSync` é um aplicativo de gerenciamento de tarefas (to-do list) desenvolvido para Android. Ele permite que os usuários se registrem, façam login e gerenciem suas tarefas pessoais, sincronizando os dados com um backend Supabase.

O projeto é construído inteiramente com tecnologias modernas de desenvolvimento Android, utilizando Kotlin e Jetpack Compose para a interface do usuário.

## Funcionalidades

Com base na estrutura de navegação definida em `MainActivity.kt`, o aplicativo possui as seguintes telas e funcionalidades:

* **Autenticação de Usuário:**
    * Tela de Login (`/login`).
    * Tela de Registro (`/register`).
* **Gerenciamento de Tarefas:**
    * **Listar Tarefas (`/tasks/{userId}`):** Exibe a lista de tarefas de um usuário específico.
    * **Adicionar Tarefa (`/add_task/{userId}`):** Permite a criação de novas tarefas.
    * **Editar Tarefa (`/edit_task/{userId}/{taskId}`):** Permite a modificação de uma tarefa existente.
    * **Excluir Tarefas:** Funcionalidade implementada na tela de lista de tarefas.
    * **Marcar como Concluída:** Permite alternar o status de uma tarefa (concluída/pendente).

## Tecnologias Utilizadas

Este projeto utiliza um stack moderno focado em Kotlin e desenvolvimento declarativo de UI:

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (utilizando Material 3)
* **Navegação:** [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
* **Backend como Serviço (BaaS):** [Supabase](https://supabase.io/)
    * Autenticação (`auth-kt`)
    * Banco de Dados (`postgrest-kt`)
    * Tempo Real (`realtime-kt`)
* **Programação Assíncrona:** Kotlin Coroutines (visto em `MainActivity.kt` com `coroutineScope.launch`)
* **Networking:** [Ktor Client](https://ktor.io/docs/client-overview.html)
* **Serialização:** [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

## Configuração do Projeto

* **Nome do App:** `NoteSync`
* **ID da Aplicação:** `com.filizzola.projeto_mobile`
* **SDK Mínimo (minSdk):** 24
* **SDK Alvo (targetSdk):** 36
* **SDK de Compilação (compileSdk):** 36
* **Versão do Java:** 11

## Como Executar

1.  Clone este repositório.
2.  Abra o projeto no Android Studio.
3.  **Configurar o Supabase:** O projeto requer credenciais do Supabase para funcionar. Atualize o arquivo `app/src/main/java/com/filizzola/projeto_mobile/MainActivity.kt` com sua URL e Chave Anônima (public-anon-key) do Supabase:

    ```kotlin
    val supabase = createSupabaseClient(
        supabaseUrl = "SUA_URL_SUPABASE",
        supabaseKey = "SUA_CHAVE_ANON_SUPABASE"
    ) {
        install(Auth)
        install(Postgrest)
    }
    ```

4.  Sincronize as dependências do Gradle.
5.  Execute o aplicativo em um emulador Android ou dispositivo físico.
