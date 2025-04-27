package com.example.criminalintent.limingxuan249400218

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

private const val ARG_DATE = "date"

class DatePickerFragment : DialogFragment() {
    interface Callbacks {
        fun onDateSelected(date: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val resultDate = GregorianCalendar(year, month, day).time
            targetFragment?.let { fragment ->
                (fragment as Callbacks).onDateSelected(resultDate)
            }
        }

        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance().apply { time = date }

        // 创建本地化的DatePickerDialog
        return DatePickerDialog(
            requireContext(),
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // 设置本地化的对话框标题
            setTitle(getLocalizedDateTitle(date))
        }
    }

    private fun getLocalizedDateTitle(date: Date): String {
        return SimpleDateFormat(
            requireContext().getString(R.string.date_format_pattern),
            Locale.getDefault()
        ).format(date)
    }

    companion object {
        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply { putSerializable(ARG_DATE, date) }
            return DatePickerFragment().apply { arguments = args }
        }
    }
}