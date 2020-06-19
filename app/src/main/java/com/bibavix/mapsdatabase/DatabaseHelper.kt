package com.bibavix.mapsdatabase
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

public class DatabaseHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_NAME(ID INTEGER PRIMARY KEY "+
                "AUTOINCREMENT, NAME TEXT,LAT_FROM REAL,LNG_FROM REAL, LAT_TO REAL, LNG_TO REAL)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(name: String, lat_from: Double, lng_from: Double, lat_to: Double, lng_to: Double){
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_2, name)
        contentValues.put(COL_3, lat_from)
        contentValues.put(COL_4, lng_from)
        contentValues.put(COL_5, lat_to)
        contentValues.put(COL_6, lng_to)
        db.insert(TABLE_NAME, null, contentValues)
    }

    fun deleteData(id: String): Int{
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "ID=?", arrayOf(id))
    }

    val allData: Cursor get() {
        val db = this.writableDatabase
        val res = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        return res
    }

    companion object{
        val DATABASE_NAME = "store.db"
        val TABLE_NAME = "product_table"
        val COL_1 = "ID"
        val COL_2 = "NAME"
        val COL_3  = "LAT_FROM"
        val COL_4  = "LNG_FROM"
        val COL_5  = "LAT_TO"
        val COL_6  = "LNG_TO"

    }

}