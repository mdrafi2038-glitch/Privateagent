package com.personalagent.admin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
class MainActivity:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val t=android.widget.TextView(this);t.text="Personal Agent Admin\\n\\nConfigure Firebase before use.";t.textSize=22f;t.setPadding(32,32,32,32);setContentView(t)}}