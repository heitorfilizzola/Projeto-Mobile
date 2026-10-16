# 📘 NoteSync — Aplicativo de Tarefas com Sincronização Offline-First

> Aplicativo Android de gerenciamento de tarefas com sincronização inteligente, funcionamento offline e arquitetura profissional baseada em **MVVM**.

O **NoteSync** permite que usuários criem, editem e excluam tarefas mesmo sem internet, armazenando tudo localmente e sincronizando automaticamente com o **Supabase** quando a conexão for restabelecida.

---

## 📱 Funcionalidades

### 🔐 Autenticação de Usuário
- Tela de Login (`/login`)
- Tela de Registro (`/register`)

### 📝 Gerenciamento de Tarefas
- Listar tarefas (`/tasks/{userId}`)
- Adicionar tarefa (`/add_task/{userId}`)
- Editar tarefa (`/edit_task/{userId}/{taskId}`)
- Excluir tarefa (na tela de lista)
- Marcar como concluída

### 🌐 Sincronização e Conectividade
- Detecta quando o dispositivo entra/sai da internet
- Reenvia operações pendentes automaticamente
- Faz merge entre registros locais e remotos
- Resolve conflitos usando **last-write-wins** baseado em `updated_at`
- Sincroniza exclusões nos dois sentidos (local ↔ remoto)
- Exibe indicador de conectividade e última sincronização

---

## 🛠️ Tecnologias Utilizadas

| Categoria               | Tecnologia                                         |
|-------------------------|----------------------------------------------------|
| Linguagem               | Kotlin                                             |
| UI                      | Jetpack Compose + Material 3                       |
| Navegação               | Navigation Compose                                 |
| Backend                 | Supabase (Auth + PostgREST + Realtime)             |
| Programação Assíncrona  | Kotlin Coroutines                                  |
| Banco Local             | Room                                               |
| Networking              | Ktor Client                                        |
| Serialização            | Kotlinx Serialization                              |
| Background Jobs         | WorkManager                                        |
| Detecção de Rede        | ConnectivityManager + NetworkCallback              |
| Logs                    | Timber                                             |
| Memory Leaks            | LeakCanary                                         |

---

## ⚙️ Configuração do Projeto

| Propriedade     | Valor                          |
|-----------------|--------------------------------|
| Nome do App     | NoteSync                       |
| ID do Pacote    | `com.filizzola.projeto_mobile` |
| `minSdk`        | 24                             |
| `targetSdk`     | 36                             |
| `compileSdk`    | 36                             |
| Java            | 11                             |

---

## ▶️ Como Executar

### 1. Clone o repositório
```bash
git clone https://github.com/SEU_USUARIO/NoteSync.git
```

### 2. Abra no Android Studio
Importe o projeto e aguarde a indexação.

### 3. Configure o Supabase
Edite as credenciais em `MainActivity.kt`:

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "SUA_URL_SUPABASE",
    supabaseKey = "SUA_CHAVE_ANON_SUPABASE"
) {
    install(Auth)
    install(Postgrest)
}
```

### 4. Sincronize o Gradle
Clique em **"Sync Now"** na barra de notificação do Android Studio.

### 5. Execute no Emulador ou Dispositivo Físico
Selecione um dispositivo e clique em **Run** (▶).

---

## 🧩 Arquitetura — MVVM

O NoteSync utiliza a arquitetura **MVVM**, garantindo separação clara entre UI, lógica de apresentação e acesso a dados.

```
┌──────────────────────────────────────────────────────┐
│                     View (UI Layer)                  │
│           Jetpack Compose — Declarativo e Reativo    │
└───────────────────────┬──────────────────────────────┘
                        │ observa StateFlow / LiveData
┌───────────────────────▼──────────────────────────────┐
│              ViewModel (Presentation Layer)           │
│  Gerencia estado, valida dados, orquestra CRUD       │
└───────────────────────┬──────────────────────────────┘
                        │ acessa repositórios
┌───────────────────────▼──────────────────────────────┐
│                Model (Data Layer)                    │
│  Repositório → LocalDataSource (Room)                │
│             → RemoteDataSource (Supabase)            │
│             → SyncDataSource (fila + retry)          │
└──────────────────────────────────────────────────────┘
```

### 🖼️ View (UI Layer)
- Implementada com Jetpack Compose de forma declarativa e reativa
- Observa `StateFlow`/`LiveData` expostos pelo ViewModel
- Reage automaticamente a atualizações
- **Não contém lógica de negócio**

### 🧠 ViewModel (Presentation Layer)
- Gerencia o estado da UI
- Valida dados de entrada
- Orquestra operações de CRUD
- Sincroniza com repositórios
- Garante ciclo de vida seguro com `viewModelScope`
- Notifica a UI sobre conectividade e status de sincronização

### 🗂️ Model (Data Layer)

**Repositórios** são responsáveis por:
- Unificar acesso a Room + Supabase
- Gerenciar a fila offline
- Resolver conflitos
- Sincronizar alterações
- Aplicar estratégias de merge
- Versionar tarefas com timestamps

**Data Sources:**

| Source             | Responsabilidade                    |
|--------------------|-------------------------------------|
| `LocalDataSource`  | Persistência local via Room         |
| `RemoteDataSource` | Comunicação com Supabase            |
| `SyncDataSource`   | Fila de operações pendentes + retry |

---

## 🔄 Sincronização Local ↔ Supabase

O NoteSync segue um padrão totalmente **offline-first**.

### 📡 1. Detecção de Conectividade
O app monitora alterações de rede usando `ConnectivityManager` + `NetworkCallback`.

**Sem internet:**
- Todas as operações são locais
- CRUD funciona normalmente
- Operações são marcadas como **pendentes**

### 📨 2. Fila de Operações (Offline)

| Ação     | Comportamento                                 |
|----------|-----------------------------------------------|
| Criação  | Adiciona ao Room e à fila de sincronização    |
| Edição   | Salva localmente e marca como pendente        |
| Exclusão | Remove localmente e adiciona à fila           |

### 🔁 3. Retorno da Conexão
O **WorkManager** dispara automaticamente:
1. Envio da fila para o Supabase
2. Download de alterações remotas
3. Merge local/remoto
4. Limpeza das marcações pendentes
5. Atualização da UI

### ⚔️ 4. Resolução de Conflitos
Estratégia: **last-write-wins** baseado em `updated_at`.

O app compara `local.updated_at` vs `remote.updated_at` e define qual versão prevalece.

### 🗑️ 5. Sincronização de Exclusões
- **Delete local** → Supabase apaga o registro
- **Delete remoto** → app apaga localmente

Bidirecional e consistente.

---

## 💾 Persistência Local — O que fica no Room

- Tarefas do usuário logado
- Campos espelhados do Supabase
- Flag `pendingSync`
- Timestamps (`created_at`, `updated_at`)
- Operações pendentes da fila

---

## 🔄 Fluxos de Dados Principais

### CRUD Offline → Sincronização
```
Usuário cria/edita/exclui
       ↓
Room grava imediatamente
       ↓
UI atualiza instantaneamente
       ↓
Operação vai para a fila (se offline)
       ↓
Quando online → fila é enviada ao Supabase
```

### Leitura Sempre Local
- A UI **sempre** lê do Room
- Quando online → Room recebe atualizações do Supabase em background

### UI 100% Reativa
- Compose reage automaticamente a alterações no banco
- Atualizações são instantâneas para o usuário

---

## 🔍 Observabilidade

- **Logs** com Timber
- **Métricas** de sincronização
- **Rastreamento de crashes** opcional com Crashlytics

---

## 🧼 Prevenção de Memory Leaks

- ViewModels independentes de `Context`
- Corrotinas canceladas corretamente no ciclo de vida
- **LeakCanary** instalado no build de debug

---

## 📄 Conclusão

O **NoteSync** é um aplicativo Android moderno, robusto e preparado para ambientes reais — suportando uso offline, quedas de internet, sincronização confiável e arquitetura escalável.
