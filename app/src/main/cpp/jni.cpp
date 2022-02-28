#include <jni.h>

//
// Created by 20123460 on 2022/2/23.
//
#include <assimp/Importer.hpp>
#include <assimp/scene.h>
#include <assimp/postprocess.h>
#include <AndroidJNIIOSystem.h>
#include <android/log.h>
#include <string>

const char *TAG = "facially";

struct Vertex {
    float ver_x;
    float ver_y;
    float ver_z;
    float tex_x;
    float tex_y;
};
struct Mesh {
    std::vector<Vertex> vertexes;
    std::vector<int> indices;
    std::vector<std::string> textures;
    bool valid = true;

    float max_x = 0;
    float max_y = 0;
    float max_z = 0;
    float min_x = 0;
    float min_y = 0;
    float min_z = 0;
};


Vertex &max_min(Mesh &mesh, Vertex &vertex) {
    mesh.max_x = mesh.max_x > vertex.ver_x ? mesh.max_x : vertex.ver_x;
    mesh.max_y = mesh.max_y > vertex.ver_y ? mesh.max_y : vertex.ver_y;
    mesh.max_z = mesh.max_z > vertex.ver_z ? mesh.max_z : vertex.ver_z;
    mesh.min_x = mesh.min_x < vertex.ver_x ? mesh.min_x : vertex.ver_x;
    mesh.min_y = mesh.min_y < vertex.ver_y ? mesh.min_y : vertex.ver_y;
    mesh.min_z = mesh.min_z < vertex.ver_z ? mesh.min_z : vertex.ver_z;
    return vertex;
}

void
with_material(const aiMaterial *material, aiTextureType type, std::vector<std::string> &textures) {
    for (int i = 0; i < material->GetTextureCount(type); ++i) {
        aiString textPath;
        aiReturn retStatus = material->GetTexture(type, i, &textPath);
        if (retStatus != aiReturn_SUCCESS || textPath.length == 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "material->GetTexture failed ");
            continue;
        }
        bool have = false;
        for (int j = 0; j < textures.size(); ++j) {
            if (textures[i] == textPath.C_Str()) have = true;
        }
        if (!have)
            textures.emplace_back(std::string(textPath.C_Str(), textPath.length));
    }
}

Mesh with_mesh(const aiMesh *mesh, const aiScene *scene) {
    Mesh result;
    std::vector<Vertex> vertexes;
    for (int i = 0; i < mesh->mNumVertices; ++i) {
        Vertex vertex{};
        memset(&vertex, 0, sizeof(vertex));
        aiVector3D mVertex = mesh->mVertices[i];
        vertex.ver_x = mVertex.x;
        vertex.ver_y = mVertex.y;
        vertex.ver_z = mVertex.z;
        if (mesh->HasTextureCoords(0)) {
            vertex.tex_x = mesh->mTextureCoords[0][i].x;
            vertex.tex_y = mesh->mTextureCoords[0][i].y;
        }
        //法线向量
        vertexes.push_back(max_min(result, vertex));
    }
    std::vector<int> indices;
    // index
    for (int i = 0; i < mesh->mNumFaces; ++i) {
        aiFace face = mesh->mFaces[i];
        if (face.mNumIndices != 3) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, " face.mNumIndices != 3 ");
            continue;
        }
        for (int j = 0; j < face.mNumIndices; ++j) {
            indices.push_back(face.mIndices[j]);
        }
    }
    std::vector<std::string> textures;
    if (mesh->mMaterialIndex >= 0) {
        const aiMaterial *material = scene->mMaterials[mesh->mMaterialIndex];
        with_material(material, aiTextureType_SPECULAR, textures);
        with_material(material, aiTextureType_DIFFUSE, textures);
        with_material(material, aiTextureType_AMBIENT, textures);
    }
    result.vertexes = vertexes;
    result.indices = indices;
    result.textures = textures;
    return result;
}

std::vector<Mesh> with_node(const aiNode *node, const aiScene *scene) {
    std::vector<Mesh> meshes;
    for (int i = 0; i < node->mNumMeshes; ++i) {
        const aiMesh *mesh = scene->mMeshes[node->mMeshes[i]];
        if (mesh) {
            Mesh m = with_mesh(mesh, scene);
            if (m.valid)
                meshes.push_back(m);
        }
    }
    for (int i = 0; i < node->mNumChildren; ++i) {
        auto all = with_node(node->mChildren[i], scene);
        meshes.insert(meshes.end(), all.begin(), all.end());
    }
    return meshes;
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_android_facially_activity_FbxActivity_loadFbx(JNIEnv *env, jobject thiz, jstring path,
                                                       jobject assert) {
    auto *importer = new Assimp::Importer();
    auto manager = AAssetManager_fromJava(env, assert);
    // 未释放
    const char *work_path = env->GetStringUTFChars(path, 0);
    auto *ioSystem = new Assimp::AndroidJNIIOSystem(work_path, manager);
    importer->SetIOHandler(ioSystem);
    //mask/13552_Pierrot_Mask_v1_l3.obj
    //mask1/uv.fbx
    const aiScene *scene = importer->ReadFile("mask/13552_Pierrot_Mask_v1_l3.obj",
                                              aiProcess_Triangulate | aiProcess_FlipUVs |
                                              aiProcess_CalcTangentSpace);

    std::vector<Mesh> meshes = with_node(scene->mRootNode, scene);
    if (scene->HasTextures()) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "-------------");
    }

    // demo only one
    Mesh one = meshes[0];
    float max_x = abs(one.max_x) > abs(one.min_x) ? abs(one.max_x) : abs(one.min_x);
    float max_y = abs(one.max_y) > abs(one.min_y) ? abs(one.max_y) : abs(one.min_y);
    float max_z = abs(one.max_z) > abs(one.min_z) ? abs(one.max_z) : abs(one.min_z);
    for (auto &vertexe : one.vertexes) {
        vertexe.ver_x /= max_x;
        vertexe.ver_y /= max_y;
        vertexe.ver_z /= max_z;
    }
    // vertex
    jfloatArray vertexes = env->NewFloatArray(one.vertexes.size() * 5);
    env->SetFloatArrayRegion(vertexes, 0, one.vertexes.size() * 5,
                             reinterpret_cast<const jfloat *>(one.vertexes.data()));
    // indices
    jintArray indices = env->NewIntArray(one.indices.size());
    env->SetIntArrayRegion(indices, 0, one.indices.size(), one.indices.data());

    // path
    jstring texPath = env->NewStringUTF(one.textures[0].data());

    // center
    jfloatArray pos = env->NewFloatArray(3);
    float center[3];
    center[0] = (one.min_x + one.max_x) / 2 / max_x;
    center[1] = (one.min_y + one.max_y) / 2 / max_y;
    center[2] = (one.min_z + one.max_z) / 2 / max_z;
    env->SetFloatArrayRegion(pos, 0, 3 , center);

    // return
    jclass cla = env->FindClass("com/android/facially/activity/Mesh");
    jmethodID method = env->GetMethodID(cla, "<init>", "([F[ILjava/lang/String;[F)V");

    return env->NewObject(cla, method, vertexes, indices, texPath, pos);
}