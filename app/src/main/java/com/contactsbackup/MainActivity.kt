package com.contactsbackup

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@RuntimePermissions
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        create.setOnClickListener({
            readContactsWithPermissionCheck()
        })

    }

    @NeedsPermission(Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public fun readContacts(){
        Toast.makeText(this,"Creating zip file",Toast.LENGTH_SHORT).show()
        val folder = File(Environment.getExternalStorageDirectory().toString() + "/Test")
        folder.mkdirs()

        val filename = folder.toString() + "/" + "Test.csv"
        val fw = FileWriter(filename)
        fw.append("Name")
        fw.append(',')
        fw.append("Number")
        fw.append(',')
        fw.append('\n')
        val startTime=System.currentTimeMillis()
        Observable.create<ContactsModel> {
            val phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null)
            while (phones.moveToNext()) {
                val name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                fw.append(name)
                fw.append(",")
                fw.append(phoneNumber)
                fw.append('\n')
            }
            fw.close()
            zip(arrayOf(filename),folder.toString()+"/test.zip")
            Log.d("done","***"+(System.currentTimeMillis()-startTime).toString())
            phones.close()
            it.onComplete()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({

        },{
            it.printStackTrace()

        },{
            val snackbar = Snackbar
                    .make(rootView, "Zip file created in folder named *TEST*", Snackbar.LENGTH_INDEFINITE)


            snackbar.show()
        })

    }

    private fun zip(_files: Array<String>, zipFileName: String) {
        try {
            val BUFFER=1024
            var origin: BufferedInputStream
            val dest = FileOutputStream(zipFileName)
            val out = ZipOutputStream(BufferedOutputStream(
                    dest))
            val data = ByteArray(BUFFER)

            for (i in _files.indices) {
                Log.v("Compress", "Adding: " + _files[i])
                val fi = FileInputStream(_files[i])
                origin = BufferedInputStream(fi, BUFFER)

                val entry = ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1))
                out.putNextEntry(entry)
                var count: Int
                count=origin.read(data,0,BUFFER)
                while (count!=-1) {
                    out.write(data, 0, count)
                    count=origin.read(data,0,BUFFER)

                }
                origin.close()
            }

            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode,grantResults)
    }
}
