package com.example.instaestepario;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.collection.BuildConfig;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class newPostFragment extends Fragment {


    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    Button publishButton;
    EditText postConentEditText;
    NavController navController;
    public AppViewModel appViewModel;
    String mediaTipo;
    Uri mediaUri;



    public newPostFragment() {}

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        publishButton = view.findViewById(R.id.publishButton);
        postConentEditText = view.findViewById(R.id.postContentEditText);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        navController = Navigation.findNavController(view);

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publicar();
            }
        });
        view.findViewById(R.id.camara_fotos).setOnClickListener(v ->
                tomarFoto());
        view.findViewById(R.id.camara_video).setOnClickListener(v ->
                tomarVideo());
        view.findViewById(R.id.grabar_audio).setOnClickListener(v ->
                grabarAudio());
        view.findViewById(R.id.imagen_galeria).setOnClickListener(v ->
                seleccionarImagen());
        view.findViewById(R.id.video_galeria).setOnClickListener(v ->
                seleccionarVideo());
        view.findViewById(R.id.audio_galeria).setOnClickListener(v ->
                seleccionarAudio());
        appViewModel.mediaSeleccionado.observe(getViewLifecycleOwner(), media ->
        {
            this.mediaUri = media.uri;
            this.mediaTipo = media.tipo;
            Glide.with(this).load(media.uri).into((android.widget.ImageView) view.findViewById(R.id.previsualizacion));
        });
    }

    private void publicar() {
        String postContent = postConentEditText.getText().toString();
        if(TextUtils.isEmpty(postContent)){
            postConentEditText.setError("Required");
            return;
        }
        publishButton.setEnabled(false);
        if (mediaTipo == null) {
            guardarEnFirestore(postContent, null);
        }
        else
        {
            pujaIguardarEnFirestore(postContent);
        }
    }
    private void guardarEnFirestore(String postContent, String mediaUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Post post = new Post(user.getUid(), user.getDisplayName(),
                (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() :
                        "R.drawable.user"), postContent,System.currentTimeMillis(), mediaUrl, mediaTipo);
        FirebaseFirestore.getInstance().collection("posts")
                .add(post)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        navController.popBackStack();
                        appViewModel.setMediaSeleccionado( null, null);
                    }
                });
    }
    private void pujaIguardarEnFirestore(final String postText) {
        FirebaseStorage.getInstance().getReference(mediaTipo + "/" +
                        UUID.randomUUID())
                .putFile(mediaUri)
                .continueWithTask(task ->
                        task.getResult().getStorage().getDownloadUrl())
                .addOnSuccessListener(url -> guardarEnFirestore(postText,
                        url.toString()));
    }

    public static newPostFragment newInstance(String param1, String param2) {
        newPostFragment fragment = new newPostFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_post, container, false);
    }
    private final ActivityResultLauncher<String> galeria =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        appViewModel.setMediaSeleccionado(uri, mediaTipo);
                    });
    private final ActivityResultLauncher<Uri> camaraFotos =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    isSuccess -> {
                        appViewModel.setMediaSeleccionado(mediaUri, "image");
                    });
    private final ActivityResultLauncher<Uri> camaraVideos =
            registerForActivityResult(new ActivityResultContracts.TakeVideo(),
                    isSuccess -> {
                        appViewModel.setMediaSeleccionado(mediaUri, "video");
                    });
    private final ActivityResultLauncher<Intent> grabadoraAudio =
            registerForActivityResult(new
                    ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    appViewModel.setMediaSeleccionado(result.getData().getData(),
                            "audio");
                }
            });
    private void seleccionarImagen() {
        mediaTipo = "image";
        galeria.launch("image/*");
    }
    private void seleccionarVideo() {
        mediaTipo = "video";
        galeria.launch("video/*");
    }
    private void seleccionarAudio() {
        mediaTipo = "audio";
        galeria.launch("audio/*");
    }
    private void tomarFoto() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    File.createTempFile("img", ".jpg",
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            );
            camaraFotos.launch(mediaUri);
        } catch (IOException e) {}
    }
    private void tomarVideo() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    File.createTempFile("vid", ".mp4",
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)));
            camaraVideos.launch(mediaUri);
        } catch (IOException e) {}
    }
    private void grabarAudio() {
        grabadoraAudio.launch(new
                Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION));
    }
}