package jp.deadend.noname.skk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import android.util.Log;

public class CustomUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private Context mContext;
    private UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;
    public CustomUncaughtExceptionHandler(Context context) {
        mContext = context;
 
        // デフォルト例外ハンドラを保持する。
        mDefaultUncaughtExceptionHandler = Thread
                .getDefaultUncaughtExceptionHandler();
    }
	@Override
	public void uncaughtException(Thread thread, Throwable ex)  {
		// TODO Auto-generated method stub
        // スタックトレースを文字列にします。
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();
        
        Log.e("ERROR", stackTrace	);
        
        // デフォルト例外ハンドラを実行し、強制終了します。
        mDefaultUncaughtExceptionHandler.uncaughtException(thread, ex);
	}

}
