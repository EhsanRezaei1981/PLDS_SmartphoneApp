package rezaei.mohammad.plds.formBuilder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.item_2_line.view.*
import rezaei.mohammad.plds.R

class SearchAdapter(private val mainItems: List<Pair<String, String?>>) :
    BaseAdapter(),
    Filterable {

    private var newItemList = mainItems

    override fun getCount(): Int {
        return newItemList.size
    }

    override fun getItem(position: Int): Any? {
        return newItemList[position].first
    }

    override fun getItemId(position: Int): Long {
        //its return real item position instead of item id
        return mainItems.indexOf(newItemList[position]).toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        return getDropDownView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_2_line, parent, false) as LinearLayout
        view.mainText.text = newItemList[position].first
        newItemList[position].second?.let {
            view.subText.visibility = View.VISIBLE
            view.subText.text = newItemList[position].second
        }
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(filter: CharSequence?): FilterResults {
                return FilterResults().apply {
                    if (filter.isNullOrEmpty()) {
                        count = mainItems.size
                        values = mainItems
                    } else {
                        mainItems.filter { it.first.contains(filter.toString(), true) }
                            .let {
                                count = it.size
                                values = it
                            }
                    }
                }
            }

            override fun publishResults(p0: CharSequence?, result: FilterResults?) {
                newItemList = result?.values as List<Pair<String, String?>>
                notifyDataSetChanged()
            }
        }
    }
}