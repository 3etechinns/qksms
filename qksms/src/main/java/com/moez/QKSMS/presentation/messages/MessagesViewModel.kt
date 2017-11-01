package com.moez.QKSMS.presentation.messages

import com.moez.QKSMS.common.di.AppComponentManager
import com.moez.QKSMS.data.model.Conversation
import com.moez.QKSMS.data.repository.MessageRepository
import com.moez.QKSMS.domain.interactor.MarkRead
import com.moez.QKSMS.domain.interactor.SendMessage
import com.moez.QKSMS.presentation.base.QkViewModel
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class MessagesViewModel(val threadId: Long) : QkViewModel<MessagesView, MessagesState>(MessagesState()) {

    @Inject lateinit var messageRepo: MessageRepository
    @Inject lateinit var sendMessage: SendMessage
    @Inject lateinit var markRead: MarkRead

    private var conversation: Conversation? = null

    init {
        AppComponentManager.appComponent.inject(this)

        disposables += sendMessage.disposables
        disposables += markRead.disposables
        disposables += messageRepo.getConversationAsync(threadId)
                .asFlowable<Conversation>()
                .filter { it.isLoaded }
                .subscribe { conversation ->
                    when (conversation.isValid) {
                        true -> {
                            this.conversation = conversation
                            val title = conversation.getTitle()
                            val messages = messageRepo.getMessages(threadId)
                            newState { it.copy(title = title, messages = messages) }
                        }
                        false -> newState { it.copy(hasError = true) }
                    }
                }

        dataChanged()
    }

    override fun bindIntents(view: MessagesView) {
        super.bindIntents(view)

        intents += view.textChangedIntent.subscribe { text ->
            newState { it.copy(draft = text.toString(), canSend = text.isNotEmpty()) }
        }

        intents += view.sendIntent.subscribe {
            val previousState = state.value!!
            sendMessage.execute(SendMessage.Params(threadId, conversation?.contacts?.get(0)?.address.orEmpty(), previousState.draft))
            newState { it.copy(draft = "", canSend = false) }
        }
    }

    fun dataChanged() {
        markRead.execute(threadId)
    }

}