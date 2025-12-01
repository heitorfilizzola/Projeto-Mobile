# NoteSync

`NoteSync` é um aplicativo de gerenciamento de tarefas (to-do list) desenvolvido para Android. Ele permite que os usuários se registrem, façam login e gerenciem suas tarefas pessoais, sincronizando os dados com Supabase.

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

## Arquitetura e Padrões de Dados

Esta seção detalha a arquitetura do software, as estratégias de sincronização de dados e os padrões de persistência adotados para garantir que o `NoteSync` seja robusto, responsivo e funcional mesmo em condições de rede instáveis.

### Arquitetura de Software

O aplicativo adota a arquitetura **MVVM (Model-View-ViewModel)**, promovendo uma clara separação de responsabilidades entre as camadas:

*   **View (UI Layer)**: Composta por Componíveis (`@Composable`) do Jetpack Compose, a View é responsável por renderizar a interface do usuário e capturar eventos de interação (cliques, gestos). Ela observa os dados expostos pelo ViewModel de forma reativa e não contém lógica de negócio.

*   **ViewModel (Domain Layer)**: Atua como um intermediário entre a View e o Data Layer. O ViewModel prepara e gerencia o estado da UI, sobrevive a mudanças de configuração (como rotação de tela) e expõe os dados para a View através de `StateFlow` ou `LiveData`. Ele contém a lógica de apresentação e delega as operações de dados (busca, escrita) para os Repositórios.

*   **Model (Data Layer)**: É a camada responsável pela gestão dos dados do aplicativo. Ela é composta por:
    *   **Repositórios**: Seguem o padrão de Repositório, atuando como a única fonte de verdade para os dados. Eles abstraem a origem dos dados (banco de dados local ou API remota), decidindo de onde buscar ou para onde salvar as informações.
    *   **Fontes de Dados (Data Sources)**: Classes que interagem diretamente com o banco de dados local (Room/SQLite) e a API remota (Supabase).

### Como funciona a sincronização local ↔ Supabase

A sincronização foi desenhada para garantir uma experiência *offline-first*.

*   **Condições de Sincronização**: A sincronização é acionada automaticamente quando o aplicativo detecta uma conexão ativa com a internet. Isso pode ocorrer no início do app, periodicamente em segundo plano, ou após a conclusão de uma operação de escrita (CRUD).

*   **Detecção de Conectividade**: O app monitora o estado da rede. Operações que exigem comunicação com o Supabase (como login ou o envio de dados em fila) só são tentadas quando o dispositivo está online.

*   **Fallback para Offline**: Quando o dispositivo está offline, o aplicativo continua totalmente funcional. Todas as operações de leitura e escrita são direcionadas exclusivamente para o banco de dados local. As operações de escrita são adicionadas a uma fila de sincronização (ou marcadas com um status "pendente").

*   **Estratégia de Merge e Resolução de Conflitos**:
    1.  **Fila de Sincronização**: Ao ficar online, um serviço em segundo plano processa a fila de operações pendentes, enviando-as para o Supabase.
    2.  **Resolução de Conflitos**: A estratégia padrão é a de **"última escrita vence" (last write wins)**, baseada em um campo `updated_at` (timestamp). Antes de enviar uma atualização, o app pode verificar se o registro no servidor foi modificado por outro dispositivo. Se a versão remota for mais nova que a base da modificação local, uma lógica de merge pode ser aplicada, embora o mais comum seja sobrescrever com o dado mais recente.

### Quais partes persistem localmente

Para suportar a funcionalidade offline, uma parte essencial dos dados é persistida localmente usando um banco de dados **Room**.

*   **Schema**: O schema do banco de dados local (Room) espelha a estrutura das tabelas do Supabase (`tasks`, `users`, etc.), armazenando apenas os dados relevantes para o usuário logado.

*   **Formatos**: Os dados são armazenados em tipos primitivos do SQLite. Tipos complexos são serializados para String (JSON) usando `TypeConverters` do Room, se necessário.

*   **Versionamento**: A evolução do schema do banco de dados é gerenciada através de **migrações do Room (`Migration`)**. Cada vez que o modelo de dados é alterado, uma nova migração é escrita para garantir que os usuários existentes não percam seus dados ao atualizar o aplicativo.

### Fluxos de Dados Principais

Os fluxos de dados foram projetados para priorizar a reatividade e a consistência.

*   **CRUD Offline → Sincronização**:
    1.  Toda operação de **C**reate, **U**pdate ou **D**elete é executada **primeiro** no banco de dados local.
    2.  A UI é atualizada imediatamente, proporcionando feedback instantâneo ao usuário.
    3.  A operação é marcada como "pendente de sincronização".
    4.  Quando houver conexão, a operação é enviada para o Supabase. Em caso de sucesso, a marcação "pendente" é removida.

*   **Leitura Sempre Local Primeiro**: Para garantir a velocidade e a disponibilidade, todas as leituras de dados (`READ`) são feitas diretamente do banco de dados local. O app ouve as atualizações do Supabase em tempo real (quando online) para atualizar a base local, que por sua vez atualiza a UI.

*   **UI Reativa**: A `View` observa `StateFlows` expostos pelo `ViewModel`. Esses fluxos são alimentados pelos Repositórios, que, por sua vez, leem do banco de dados local. Qualquer alteração nos dados locais (seja por uma ação do usuário ou uma sincronização em background) é propagada automaticamente até a UI, que se recompõe para exibir o estado mais recente.