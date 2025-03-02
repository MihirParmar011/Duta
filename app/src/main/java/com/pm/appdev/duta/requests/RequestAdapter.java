package com.pm.appdev.duta.requests;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pm.appdev.duta.Common.Constants;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.R;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private Context context;
    private List<RequestModel> requestModelList;
    private DatabaseReference databaseReferenceFriendRequests, databaseReferenceChats;
    private FirebaseUser currentUser;

    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_layout, parent, false);
        return new RequestViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RequestModel requestModel = requestModelList.get(position);

        // Set user name
        holder.tvFullName.setText(requestModel.getUserName());

        // Handle accept button click
        holder.btnAcceptRequest.setOnClickListener(v -> {
            holder.pbDecision.setVisibility(View.VISIBLE);
            holder.btnAcceptRequest.setVisibility(View.GONE);
            holder.btnDenyRequest.setVisibility(View.GONE);

            // Update the request status to "accepted"
            databaseReferenceFriendRequests.child(currentUser.getUid())
                    .child(requestModel.getUserId())
                    .child(NodeNames.REQUEST_TYPE)
                    .setValue(Constants.REQUEST_STATUS_ACCEPTED)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            databaseReferenceFriendRequests.child(requestModel.getUserId())
                                    .child(currentUser.getUid())
                                    .child(NodeNames.REQUEST_TYPE)
                                    .setValue(Constants.REQUEST_STATUS_ACCEPTED)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            // Add users to chats node
                                            String chatKey = databaseReferenceChats.push().getKey();
                                            if (chatKey != null) {
                                                databaseReferenceChats.child(currentUser.getUid())
                                                        .child(requestModel.getUserId())
                                                        .child(NodeNames.CHAT_ID) // Use the constant here
                                                        .setValue(chatKey);

                                                databaseReferenceChats.child(requestModel.getUserId())
                                                        .child(currentUser.getUid())
                                                        .child(NodeNames.CHAT_ID) // Use the constant here
                                                        .setValue(chatKey);

                                                Toast.makeText(context, "Friend request accepted.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(context, "Failed to accept friend request.", Toast.LENGTH_SHORT).show();
                                        }
                                        holder.pbDecision.setVisibility(View.GONE);
                                    });
                        } else {
                            Toast.makeText(context, "Failed to accept friend request.", Toast.LENGTH_SHORT).show();
                            holder.pbDecision.setVisibility(View.GONE);
                        }
                    });
        });

        // Handle deny button click
        holder.btnDenyRequest.setOnClickListener(v -> {
            holder.pbDecision.setVisibility(View.VISIBLE);
            holder.btnAcceptRequest.setVisibility(View.GONE);
            holder.btnDenyRequest.setVisibility(View.GONE);

            // Remove the friend request
            databaseReferenceFriendRequests.child(currentUser.getUid())
                    .child(requestModel.getUserId())
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            databaseReferenceFriendRequests.child(requestModel.getUserId())
                                    .child(currentUser.getUid())
                                    .removeValue()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(context, "Friend request denied.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(context, "Failed to deny friend request.", Toast.LENGTH_SHORT).show();
                                        }
                                        holder.pbDecision.setVisibility(View.GONE);
                                    });
                        } else {
                            Toast.makeText(context, "Failed to deny friend request.", Toast.LENGTH_SHORT).show();
                            holder.pbDecision.setVisibility(View.GONE);
                        }
                    });
        });
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName;
        Button btnAcceptRequest, btnDenyRequest;
        ProgressBar pbDecision;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnDenyRequest = itemView.findViewById(R.id.btnDenyRequest);
            pbDecision = itemView.findViewById(R.id.pbDecision);
        }
    }
}


//package com.pm.appdev.duta.requests;
//
//import android.content.Context;
//import android.net.Uri;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.pm.appdev.duta.R;
//import com.pm.appdev.duta.Common.Constants;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.pm.appdev.duta.Common.Util;
//
//import com.bumptech.glide.Glide;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ServerValue;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//
//import java.util.List;
//
//public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {
//
//    private Context context;
//    private List<RequestModel>  requestModelList;
//    private DatabaseReference databaseReferenceFriendRequests, databaseReferenceChats;
//    private FirebaseUser currentUser;
//
//
//    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
//        this.context = context;
//        this.requestModelList = requestModelList;
//    }
//
//    @NonNull
//    @Override
//    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_layout,parent, false);
//        return new RequestViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final RequestViewHolder holder, int position) {
//
//        final RequestModel requestModel = requestModelList.get(position);
//
//        holder.tvFullName.setText(requestModel.getUserName());
//        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER + "/" +  requestModel.getPhotoName());
//
//        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//            @Override
//            public void onSuccess(Uri uri) {
//
//                Glide.with(context)
//                        .load(uri)
//                        .placeholder(R.drawable.default_profile)
//                        .error(R.drawable.default_profile)
//                        .into(holder.ivProfile);
//
//            }
//        });
//
//        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
//        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//
//        holder.btnAcceptRequest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                holder.pbDecision.setVisibility(View.VISIBLE);
//                holder.btnDenyRequest.setVisibility(View.GONE);
//                holder.btnAcceptRequest.setVisibility(View.GONE);
//
//                final String userId = requestModel.getUserId();
//                databaseReferenceChats.child(currentUser.getUid()).child(userId)
//                        .child(NodeNames.TIME_STAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                   if(task.isSuccessful())
//                   {
//                       databaseReferenceChats.child(userId).child(currentUser.getUid())
//                               .child(NodeNames.TIME_STAMP).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
//                           @Override
//                           public void onComplete(@NonNull Task<Void> task) {
//                               if(task.isSuccessful()){
//                                   databaseReferenceFriendRequests.child(currentUser.getUid()).child(userId)
//                                           .child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                       @Override
//                                       public void onComplete(@NonNull Task<Void> task) {
//                                      if(task.isSuccessful())
//                                      {
//                                          databaseReferenceFriendRequests.child(userId).child(currentUser.getUid())
//                                                  .child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                              @Override
//                                              public void onComplete(@NonNull Task<Void> task) {
//
//                                                  if(task.isSuccessful())
//                                                  {
//
//                                                      String title = "Friend Request Accepted";
//                                                      String message= "Friend request accepted by " + currentUser.getDisplayName();
//                                                      Util.sendNotification(context, title, message, userId);
//
//                                                      holder.pbDecision.setVisibility(View.GONE);
//                                                      holder.btnDenyRequest.setVisibility(View.VISIBLE);
//                                                      holder.btnAcceptRequest.setVisibility(View.VISIBLE);
//
//                                                  }
//                                                  else
//                                                  {
//                                                      handleException(holder, task.getException());
//                                                  }
//                                              }
//                                          });
//                                      }
//                                      else{
//                                          handleException(holder, task.getException());
//                                      }
//                                       }
//                                   });
//
//                               }
//                               else{
//                                   handleException(holder, task.getException());
//                               }
//                           }
//                       });
//
//
//                   }
//                   else
//                   {
//                        handleException(holder, task.getException());
//
//                   }
//
//                    }
//                });
//
//
//            }
//        });
//
//
//        holder.btnDenyRequest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                holder.pbDecision.setVisibility(View.VISIBLE);
//                holder.btnDenyRequest.setVisibility(View.GONE);
//                holder.btnAcceptRequest.setVisibility(View.GONE);
//
//                final String userId = requestModel.getUserId();
//                databaseReferenceFriendRequests.child(currentUser.getUid()).child(userId)
//                        .child(NodeNames.REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//
//                        if(task.isSuccessful()){
//                            databaseReferenceFriendRequests.child(userId).child(currentUser.getUid())
//                                    .child(NodeNames.REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                @Override
//                                public void onComplete(@NonNull Task<Void> task) {
//
//                                    if(task.isSuccessful())
//                                    {
//                                        Toast.makeText(context, R.string.request_denied_successfully, Toast.LENGTH_SHORT).show();
//
//                                        String title = "Friend Request Denied";
//                                        String message= "Friend request denied by " + currentUser.getDisplayName();
//                                        Util.sendNotification(context, title, message, userId);
//
//
//                                        holder.pbDecision.setVisibility(View.GONE);
//                                        holder.btnDenyRequest.setVisibility(View.VISIBLE);
//                                        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
//                                    }
//                                    else
//                                    {
//                                        Toast.makeText(context, context.getString( R.string.failed_to_deny_request, task.getException()) , Toast.LENGTH_SHORT).show();
//                                        holder.pbDecision.setVisibility(View.GONE);
//                                        holder.btnDenyRequest.setVisibility(View.VISIBLE);
//                                        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
//                                    }
//                                }
//                            });
//                        }
//                        else
//                        {
//                            Toast.makeText(context, context.getString( R.string.failed_to_deny_request, task.getException()) , Toast.LENGTH_SHORT).show();
//                            holder.pbDecision.setVisibility(View.GONE);
//                            holder.btnDenyRequest.setVisibility(View.VISIBLE);
//                            holder.btnAcceptRequest.setVisibility(View.VISIBLE);
//
//                        }
//                    }
//                });
//
//
//            }
//        });
//    }
//
//    private void handleException(RequestViewHolder holder,  Exception exception) {
//        Toast.makeText(context,  context.getString( R.string.failed_to_accept_request, exception) , Toast.LENGTH_SHORT).show();
//        holder.pbDecision.setVisibility(View.GONE);
//        holder.btnDenyRequest.setVisibility(View.VISIBLE);
//        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
//    }
//
//    @Override
//    public int getItemCount() {
//        return requestModelList.size();
//    }
//
//    public class RequestViewHolder extends  RecyclerView.ViewHolder {
//
//        private TextView tvFullName;
//        private ImageView ivProfile;
//        private Button btnAcceptRequest, btnDenyRequest;
//        private ProgressBar pbDecision;
//
//
//        public RequestViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvFullName = itemView.findViewById(R.id.tvFullName);
//            ivProfile = itemView.findViewById(R.id.ivProfile);
//            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
//            btnDenyRequest = itemView.findViewById(R.id.btnDenyRequest);
//            pbDecision  = itemView.findViewById(R.id.pbDecision);
//
//        }
//    }
//}
