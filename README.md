📘 NoteSync — Aplicativo de Tarefas com Sincronização Offline-First e Supabase

NoteSync é um aplicativo de gerenciamento de tarefas desenvolvido para Android, com foco em sincronização inteligente, funcionamento offline, e arquitetura profissional baseada em MVVM.
O app permite que usuários criem, editem e excluam tarefas mesmo sem internet, armazenando tudo localmente e sincronizando automaticamente com o Supabase quando a conexão for restabelecida.

Construído inteiramente com tecnologias modernas do ecossistema Android, o NoteSync utiliza Kotlin, Jetpack Compose, Room, Ktor Client, WorkManager e o backend Supabase.

📱 Funcionalidades

Com base na estrutura de navegação definida em MainActivity.kt, o aplicativo oferece:

🔐 Autenticação de Usuário

Tela de Login (/login)

Tela de Registro (/register)

📝 Gerenciamento de Tarefas

Listar Tarefas (/tasks/{userId})

Adicionar Tarefa (/add_task/{userId})

Editar Tarefa (/edit_task/{userId}/{taskId})

Excluir Tarefa (na tela de lista)

Marcar como concluída

Persistência offline (Room)

Sincronização com Supabase

Detecção de conectividade e operação offline-first

Fila de sincronização automática

🌐 Sincronização e Conectividade

Detecta quando o dispositivo entra/saí da internet

Reenvia operações pendentes automaticamente

Faz merge entre registros locais e remotos

Resolve conflitos usando last_write_wins baseado em updated_at

Sincroniza exclusões nos dois sentidos

Exibe indicador de conectividade e última sincronização

🛠️ Tecnologias Utilizadas
Categoria	Tecnologia
Linguagem	Kotlin
UI	Jetpack Compose + Material 3
Navegação	Navigation Compose
Backend	Supabase (Auth + Postgrest + Realtime)
Programação Assíncrona	Kotlin Coroutines
Banco Local	Room
Networking	Ktor Client
Serialização	Kotlinx Serialization
Background Jobs	WorkManager
Detecção de Rede	ConnectivityManager + NetworkCallback
Logs	Timber
Memory Leaks	LeakCanary
⚙️ Configuração do Projeto

Nome do App: NoteSync

ID do Pacote: com.filizzola.projeto_mobile

minSdk: 24

targetSdk: 36

compileSdk: 36

Java: 11

▶ Como Executar

Clone o repositório

git clone https://github.com/SEU_USUARIO/NoteSync.git


Abra no Android Studio

Configurar Supabase

Edite as credenciais em MainActivity.kt:

val supabase = createSupabaseClient(
    supabaseUrl = "SUA_URL_SUPABASE",
    supabaseKey = "SUA_CHAVE_ANON_SUPABASE"
) {
    install(Auth)
    install(Postgrest)
}


Sincronize o Gradle

Execute no Emulador ou Dispositivo Físico

🧩 Arquitetura e Padrões de Dados

O NoteSync utiliza a arquitetura MVVM, garantindo separação clara entre UI, lógica de apresentação e acesso a dados.

🖼 1. View (UI Layer)

Implementada com Jetpack Compose, funciona de forma declarativa e reativa:

Observa StateFlow/LiveData expostos pelo ViewModel

Reage automaticamente a atualizações

Não contém lógica de negócio

🧠 2. ViewModel (Domain/Presentation Layer)

Responsável por:

Gerenciar o estado da UI

Validar dados

Orquestrar operações de CRUD

Sincronizar com repositórios

Garantir ciclo de vida seguro com viewModelScope

Notificar a UI sobre conectividade e status de sincronização

🗂 3. Model (Data Layer)

Inclui:

Repositórios

Responsáveis por:

Unificar acesso a Room + Supabase

Gerenciar fila offline

Resolver conflitos

Sincronizar alterações

Aplicar estratégias de merge

Versionar tarefas com timestamps

Data Sources

LocalDataSource (Room)

RemoteDataSource (Supabase)

SyncDataSource (fila + políticas de retry)

🔄 Sincronização Local ↔ Supabase

A sincronização do NoteSync segue um padrão totalmente offline-first.

📡 1. Detecção de Conectividade

O app monitora alterações de rede usando:

ConnectivityManager

NetworkCallback

Sem internet:

Todas as operações são locais

CRUD funciona normalmente

Operações são marcadas como pendentes

📨 2. Fila de Operações

Quando offline:

Criação → adiciona ao Room e à fila

Edição → salva localmente e marca pendente

Exclusão → remove localmente e adiciona operação na fila

🔁 3. Quando volta a internet

O WorkManager dispara automaticamente:

Envio da fila

Download de alterações do Supabase

Merge local/remoto

Limpeza das marcações pendentes

Atualização da UI

⚔ 4. Resolução de Conflitos

Estratégia: last write wins (updated_at)

O app compara:

local.updated_at

remote.updated_at

E define qual versão prevalece.

🗑 5. Sincronização de Exclusões

Delete local → Supabase apaga

Delete remoto → app apaga localmente

Bidirecional e consistente.

💾 Persistência Local — O que fica no Room

Tarefas do usuário logado

Campos espelhados do Supabase

Flag pendingSync

Timestamps (created_at, updated_at)

Operações pendentes

🔄 Fluxos de Dados Principais
CRUD Offline → Sincronização

Usuário cria/edita/exclui

Room grava imediatamente

UI atualiza instantaneamente

Operação vai para a fila (offline)

Quando online → fila é enviada para Supabase

Leitura Sempre Local

A UI lê sempre do Room

Quando online → Room recebe atualizações do Supabase

UI 100% Reativa

Compose reage a alterações no banco

Atualizações são instantâneas

🔍 Observabilidade

Logs com Timber

Métricas de sincronização

Tracking opcional com Crashlytics

🧼 Prevenção de Memory Leaks

ViewModels independentes de Context

Corrotinas canceladas

LeakCanary instalado no build de debug

📄 Conclusão

O NoteSync é um aplicativo Android moderno, robusto e preparado para ambientes reais, suportando uso offline, queda de internet, sincronização confiável e arquitetura escalável.
