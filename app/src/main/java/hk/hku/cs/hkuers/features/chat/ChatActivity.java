package hk.hku.cs.hkuers.features.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.models.Message;

import java.util.Date;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText etMessage;
    private FirebaseFirestore db;
    private String groupId;
    private FirestoreRecyclerAdapter<Message, MessageViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化组件
        db = FirebaseFirestore.getInstance();
        groupId = getIntent().getStringExtra("groupId");
        recyclerView = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setupMessageAdapter();

        // 发送消息
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
    }

    private void setupMessageAdapter() {
        Query query = db.collection("groups").document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Message, MessageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull Message model) {
                holder.bind(model);
            }

            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message, parent, false);
                return new MessageViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(adapter.getItemCount() - 1); // 滚动到底部
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String senderEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        Message message = new Message(
                senderId,
                senderEmail,
                messageText,
                new Timestamp(new Date())
        );

        db.collection("groups").document(groupId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> etMessage.setText(""))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    // ViewHolder类
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSentMessage, tvSentTime, tvReceivedMessage, tvReceivedTime;
        private View layoutSent, layoutReceived;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
        }

        public void bind(Message message) {
            // 判断消息发送者是否是当前用户
            boolean isSentByMe = message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid());

            layoutSent.setVisibility(isSentByMe ? View.VISIBLE : View.GONE);
            layoutReceived.setVisibility(isSentByMe ? View.GONE : View.VISIBLE);

            if (isSentByMe) {
                tvSentMessage.setText(message.getText());
                tvSentTime.setText(formatTime(message.getTimestamp()));
            } else {
                tvReceivedMessage.setText(message.getText());
                tvReceivedTime.setText(formatTime(message.getTimestamp()));
            }
        }

        private String formatTime(Timestamp timestamp) {
            return new java.text.SimpleDateFormat("HH:mm").format(timestamp.toDate());
        }
    }
}