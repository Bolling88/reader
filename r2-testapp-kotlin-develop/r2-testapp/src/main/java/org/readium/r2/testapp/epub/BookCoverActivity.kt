package org.readium.r2.testapp.epub

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.indeterminateProgressDialog
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.audio.AudioBookParser
import org.readium.r2.streamer.parser.cbz.CBZParser
import org.readium.r2.streamer.parser.divina.DiViNaParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Book
import org.readium.r2.testapp.db.books
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import org.readium.r2.testapp.utils.ContentResolverUtil
import org.zeroturnaround.zip.ZipUtil
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.util.*
import java.util.zip.ZipException
import kotlin.coroutines.CoroutineContext

class BookCoverActivity : AppCompatActivity(), CoroutineScope {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private lateinit var R2DIRECTORY: String
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions
    private lateinit var preferences: SharedPreferences

    protected lateinit var server: Server
    private var localPort: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_cover)

        val s = ServerSocket(0)
        s.localPort
        s.close()

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        localPort = s.localPort
        server = Server(localPort)

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        val properties = Properties()
        val inputStream = this.assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir = properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        R2DIRECTORY = if (useExternalFileDir) {
            this.getExternalFilesDir(null)?.path + "/"
        } else {
            this.filesDir.path + "/"
        }
    }

    override fun onStart() {
        super.onStart()
        startServer()
        parseIntentPublication("http://www.gutenberg.org/cache/epub/61253/pg61253.epub")
    }

    private fun parseIntentPublication(uriString: String) {
        val uri: Uri? = Uri.parse(uriString)
        if (uri != null) {

            val fileName = UUID.randomUUID().toString()
            val publicationPath = R2DIRECTORY + fileName

            task {
                ContentResolverUtil.getContentInputStream(this, uri, publicationPath)
            } then {
                preparePublication(publicationPath, uriString, fileName)
            }

        }
    }


    private fun preparePublication(publicationPath: String, uriString: String, fileName: String) {

        val file = File(publicationPath)

        try {
            launch {

                when {
                    uriString.endsWith(Publication.EXTENSION.EPUB.value) -> {
                        val parser = EpubParser()
                        val pub = parser.parse(publicationPath)
                        if (pub != null) {
                            prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true }
                                ?: false)
                        }
                    }
                    uriString.endsWith(Publication.EXTENSION.CBZ.value) -> {
                        val parser = CBZParser()
                        val pub = parser.parse(publicationPath)
                        if (pub != null) {
                            prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true }
                                ?: false)
                        }
                    }
                    uriString.endsWith(Publication.EXTENSION.AUDIO.value) -> {
                        val parser = AudioBookParser()
                        val pub = parser.parse(publicationPath)
                        if (pub != null) {
                            prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true }
                                ?: false)
                        }
                    }
                    uriString.endsWith(Publication.EXTENSION.DIVINA.value) -> {
                        val parser = DiViNaParser()
                        val pub = parser.parse(publicationPath)
                        if (pub != null) {
                            prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true }
                                ?: false)
                        }
                    }


                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    protected fun prepareToServe(pub: PubBox?, fileName: String, absolutePath: String, add: Boolean, lcp: Boolean) {
        if (pub == null) {
            Toast.makeText(this, "Invalid publication", Toast.LENGTH_LONG).show()
            return
        }
        val publication = pub.publication
        val container = pub.container

        launch {
            val publicationIdentifier = publication.metadata.identifier!!
            val book: Book = when (publication.type) {
                Publication.TYPE.EPUB -> {
                    preferences.edit().putString("$publicationIdentifier-publicationPort", localPort.toString()).apply()
                    val author = authorName(publication)
                    val cover = publication.coverLink?.href?.let {
                        try {
                            ZipUtil.unpackEntry(File(absolutePath), it.removePrefix("/"))
                        } catch (e: ZipException) {
                            null
                        }
                    }

                    if (!lcp) {
                        server.addEpub(publication, container, "/$fileName", applicationContext.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json")
                    }
                    Book(title = publication.metadata.title, author = author, href = absolutePath, identifier = publicationIdentifier, cover = cover, ext = Publication.EXTENSION.EPUB, progression = "{}")
                }
                Publication.TYPE.CBZ -> {
                    val cover = publication.coverLink?.href?.let {
                        try {
                            container.data(it)
                        } catch (e: ContainerError.fileNotFound) {
                            null
                        }
                    }

                    Book(title = publication.metadata.title, href = absolutePath, identifier = publicationIdentifier, cover = cover, ext = Publication.EXTENSION.CBZ, progression = "{}")
                }
                Publication.TYPE.DiViNa -> {
                    val cover = publication.coverLink?.href?.let {
                        try {
                            container.data(it)
                        } catch (e: ContainerError.fileNotFound) {
                            null
                        }
                    }

                    Book(title = publication.metadata.title, href = absolutePath, identifier = publicationIdentifier, cover = cover, ext = Publication.EXTENSION.DIVINA, progression = "{}")
                }
                Publication.TYPE.AUDIO -> {
                    val cover = publication.coverLink?.href?.let {
                        try {
                            container.data(it)
                        } catch (e: ContainerError.fileNotFound) {
                            null
                        }
                    }

                    //Building book object and adding it to library
                    Book(title = publication.metadata.title, href = absolutePath, identifier = publicationIdentifier, cover = cover, ext = Publication.EXTENSION.AUDIO, progression = "{}")
                }
                else -> TODO()
            }
            startActivity(absolutePath, book, publication)
        }
    }

    private fun startActivity(publicationPath: String, book: Book, publication: Publication, coverByteArray: ByteArray? = null) {
        val intent = Intent(this, when (publication.type) {
            else -> EpubActivity::class.java
        })
        intent.putExtra("publicationPath", publicationPath)
        intent.putExtra("publicationFileName", book.fileName)
        intent.putExtra("publication", publication)
        intent.putExtra("bookId", book.id)
        intent.putExtra("cover", coverByteArray)

        startActivity(intent)
    }

    private fun authorName(publication: Publication): String {
        return publication.metadata.authors.firstOrNull()?.name?.let {
            return@let it
        } ?: run {
            return@run String()
        }
    }

    private fun startServer() {
        if (!server.isAlive) {
            try {
                server.start()
            } catch (e: IOException) {
                // do nothing
                if (BuildConfig.DEBUG) Timber.e(e)
            }
            if (server.isAlive) {

                // Add Resources from R2Navigator
                server.loadReadiumCSSResources(assets)
                server.loadR2ScriptResources(assets)
                server.loadR2FontResources(assets, applicationContext)

//                // Add your own resources here
//                server.loadCustomResource(assets.open("scripts/test.js"), "test.js")
//                server.loadCustomResource(assets.open("styles/test.css"), "test.css")
//                server.loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")

                server.loadCustomResource(assets.open("Search/mark.js"), "mark.js", Injectable.Script)
                server.loadCustomResource(assets.open("Search/search.js"), "search.js", Injectable.Script)
                server.loadCustomResource(assets.open("Search/mark.css"), "mark.css", Injectable.Style)
                server.loadCustomResource(assets.open("scripts/crypto-sha256.js"), "crypto-sha256.js", Injectable.Script)
                server.loadCustomResource(assets.open("scripts/highlight.js"), "highlight.js", Injectable.Script)


            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
        }
    }
}
