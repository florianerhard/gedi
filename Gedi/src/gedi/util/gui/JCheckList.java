/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.util.gui;

import gedi.util.functions.IntBooleanConsumer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Components, that shows a list with check boxes. The selection is
 * directly reflected to the checked array of the constructor.
 * 
 * @author Florian Erhard
 *
 */
public class JCheckList extends JScrollPane {
	private JTable table;
	private String listName;
	private Object[] listData;
	private boolean[] checked;
	private IntBooleanConsumer callback;
	
	/**
	 * Creates a JCheckList. Results are reflected to the checked parameter.
	 * 
	 * @param listName the name of the list
	 * @param listData the list items
	 * @param checked the checked items
	 */
	public JCheckList(String listName, Object[] listData, boolean[] checked, IntBooleanConsumer callback) {
		super(new JTable());
		
		this.listName = listName;
		this.listData = listData;
		this.checked = checked;
		this.callback = callback;
		table = (JTable) getViewport().getView();
		table.setModel(new SelectionTableModel());
		
		table.getColumn("").setPreferredWidth(new JCheckBox().getWidth());
		setPreferredSize(new Dimension(300,500));
	}
	
	
	
	private class SelectionTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 5766854849047783054L;

		@Override
		public String getColumnName(int column) {
			if (column==0) return "";
			else return listName;
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex==0) return Boolean.class;
			else return Object.class;
		}
		
		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			return listData.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex==0) return checked[rowIndex];
			else return listData[rowIndex];
		}
		
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			checked[rowIndex] = (Boolean)value;
			callback.accept(rowIndex, (Boolean)value);
			fireTableCellUpdated(rowIndex, columnIndex);
		}
		
		
	}

	public class MyCheckboxRenderer extends JCheckBox implements TableCellRenderer{

		private static final long serialVersionUID = -5481575640973211874L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			return this;
		}
	
	}

}
