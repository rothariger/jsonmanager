package com.quamove.jsonmanager.adapters;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.johnkil.print.PrintView;
import com.quamove.jsonmanager.R;
import com.quamove.jsonmanager.data.tree.ArrayItem;
import com.quamove.jsonmanager.data.tree.BaseItem;
import com.quamove.jsonmanager.data.tree.ObjectItem;
import com.quamove.jsonmanager.data.tree.PropertyItem;

import java.util.Set;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;

/**
 * This is a very simple adapter that provides very basic tree view with a
 * checkboxes and simple item description.
 */
public class JsonAdapter extends AbstractTreeViewAdapter<BaseItem> {

    private final Set<BaseItem> selected;

    public JsonAdapter(final Activity context,
                       final Set<BaseItem> selected,
                       final TreeStateManager<BaseItem> treeStateManager,
                       final int numberOfLevels) {
        super(context, treeStateManager, numberOfLevels);
        this.selected = selected;
    }

    @Override
    public View getNewChildView(final TreeNodeInfo<BaseItem> treeNodeInfo) {
        final RelativeLayout viewLayout = (RelativeLayout) getActivity()
                .getLayoutInflater().inflate(R.layout.layout_array_node, null);
        return updateView(viewLayout, treeNodeInfo);
    }

    @Override
    public RelativeLayout updateView(final View view,
                                     final TreeNodeInfo<BaseItem> treeNodeInfo) {
        final RelativeLayout viewLayout = (RelativeLayout) view;
        final ControlsHolder cHolder = new ControlsHolder(view);
        resetView(cHolder);

        cHolder.getArrowIcon().setVisibility(View.VISIBLE);
        if (treeNodeInfo.isWithChildren())
            if (treeNodeInfo.isExpanded())
                cHolder.getArrowIcon().setIconTextRes(R.string.ic_keyboard_arrow_down);
            else
                cHolder.getArrowIcon().setIconTextRes(R.string.ic_keyboard_arrow_right);
        else
            cHolder.getArrowIcon().setVisibility(View.INVISIBLE);

        cHolder.getIcon().setIconTextRes(treeNodeInfo.getId().icon);

        cHolder.getTNodeName().setText(treeNodeInfo.getId().name);
        if (treeNodeInfo.getId() instanceof PropertyItem) {
            cHolder.getTNodeValue().setText(treeNodeInfo.getId().getAsPropertyItem().value);
            cHolder.getTNodeValue().setVisibility(View.VISIBLE);
        }

        cHolder.getBtnEdit().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                treeNodeInfo.getId().isEdit = true;
                cHolder.getViewSwitcher().showNext();
                cHolder.getENodeName().setText(cHolder.getTNodeName().getText());
                if (treeNodeInfo.getId() instanceof PropertyItem) {
                    cHolder.getENodeValue().setVisibility(View.VISIBLE);
                    cHolder.getENodeValue().setText(cHolder.getTNodeValue().getText());
                } else {
                    cHolder.getENodeValue().setVisibility(View.GONE);
                }
                cHolder.getButtons().setVisibility(View.GONE);
            }
        });

        cHolder.getBtnAddArray().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseItem newNode;
                if (treeNodeInfo.getId() instanceof ArrayItem)
                    newNode = new ArrayItem(String.valueOf(getManager().getChildren(treeNodeInfo.getId()).size()));
                else
                    newNode = new ArrayItem("New Array");

                getManager().addAfterChild(treeNodeInfo.getId(), newNode, null);
            }
        });

        cHolder.getBtnAddObject().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseItem newNode;
                if (treeNodeInfo.getId() instanceof ArrayItem)
                    newNode = new ObjectItem(String.valueOf(getManager().getChildren(treeNodeInfo.getId()).size()));
                else
                    newNode = new ObjectItem("New Object");
                getManager().addAfterChild(treeNodeInfo.getId(), newNode, null);
            }
        });

        cHolder.getBtnAddProperty().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseItem newNode;
                newNode = new PropertyItem("");
                getManager().addAfterChild(treeNodeInfo.getId(), newNode, null);
            }
        });

        view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getManager().removeNodeRecursively(treeNodeInfo.getId());
            }
        });

        cHolder.getAcceptButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                treeNodeInfo.getId().isEdit = false;
                cHolder.getViewSwitcher().showNext();
                cHolder.getTNodeName().setText(cHolder.getENodeName().getText());
                treeNodeInfo.getId().name = cHolder.getENodeName().getText().toString();
                if (treeNodeInfo.getId() instanceof PropertyItem) {
                    ((PropertyItem) treeNodeInfo.getId()).value = cHolder.getENodeValue().getText().toString();
                    cHolder.getTNodeValue().setText(cHolder.getENodeValue().getText());
                }
                cHolder.getButtons().setVisibility(View.VISIBLE);
            }
        });

        cHolder.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                treeNodeInfo.getId().isEdit = false;
                cHolder.getViewSwitcher().showNext();
                cHolder.getButtons().setVisibility(View.VISIBLE);
            }
        });

        if (getManager().getParent(treeNodeInfo.getId()) instanceof ArrayItem) {
            cHolder.getBtnEdit().setVisibility(View.GONE);
        }

        if (treeNodeInfo.getId() instanceof ArrayItem) {
            cHolder.getBtnAddProperty().setVisibility(View.GONE);
        }

        if (treeNodeInfo.getId() instanceof PropertyItem) {
            cHolder.getBtnAddArray().setVisibility(View.GONE);
            cHolder.getBtnAddObject().setVisibility(View.GONE);
            cHolder.getBtnAddProperty().setVisibility(View.GONE);
        }

        if (treeNodeInfo.getLevel() == 0) {
            cHolder.getBtnEdit().setVisibility(View.GONE);
            cHolder.getBtnDelete().setVisibility(View.GONE);
        }

        if (treeNodeInfo.getId().isEdit) {
            cHolder.getBtnEdit().callOnClick();
        }

        return viewLayout;
    }

    private void resetView(ControlsHolder cHolder) {
        cHolder.getViewSwitcher().setDisplayedChild(0);

        cHolder.getButtons().setVisibility(View.VISIBLE);
        cHolder.getBtnDelete().setVisibility(View.VISIBLE);
        cHolder.getBtnEdit().setVisibility(View.VISIBLE);
        cHolder.getBtnAddArray().setVisibility(View.VISIBLE);
        cHolder.getBtnAddObject().setVisibility(View.VISIBLE);
        cHolder.getBtnAddProperty().setVisibility(View.VISIBLE);
        cHolder.getTNodeValue().setVisibility(View.GONE);

        cHolder.getTNodeValue().setText("");
        cHolder.getTNodeName().setText("");
    }


    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class ControlsHolder {
        private View _view;
        private ViewSwitcher _viewSwitcher;
        private PrintView _arrow_icon;
        private PrintView _icon;
        private PrintView _btn_edit;
        private PrintView _btn_addArray;
        private PrintView _btn_addObject;
        private PrintView _btn_addProperty;
        private PrintView _btn_delete;
        private PrintView _etb_Accept;
        private PrintView _etb_Cancel;
        private TextView _tvnode_name;
        private EditText _etnode_name;
        private TextView _tvnode_value;
        private EditText _etnode_value;
        private View _buttons;


        public ControlsHolder(View view) {
            _view = view;
            _viewSwitcher = (ViewSwitcher) _view.findViewById(R.id.node_value);
            _arrow_icon = (PrintView) _view.findViewById(R.id.arrow_icon);
            _icon = (PrintView) _view.findViewById(R.id.icon);
            _btn_edit = (PrintView) _view.findViewById(R.id.btn_edit);
            _btn_addArray = (PrintView) _view.findViewById(R.id.btn_addArray);
            _btn_addObject = (PrintView) _view.findViewById(R.id.btn_addObject);
            _btn_addProperty = (PrintView) _view.findViewById(R.id.btn_addProperty);
            _btn_delete = (PrintView) view.findViewById(R.id.btn_delete);
            _tvnode_name = (TextView) _view.findViewById(R.id.tvnode_name);
            _etnode_name = (EditText) _view.findViewById(R.id.etnode_name);
            _tvnode_value = (TextView) _view.findViewById(R.id.tvnode_value);
            _etnode_value = (EditText) _view.findViewById(R.id.etnode_value);
            _buttons = _view.findViewById(R.id.buttonsContainer);
            _etb_Accept = (PrintView) view.findViewById(R.id.etb_Accept);
            _etb_Cancel = (PrintView) view.findViewById(R.id.etb_Cancel);
        }

        public ViewSwitcher getViewSwitcher() {
            return _viewSwitcher;
        }

        public PrintView getArrowIcon() {
            return _arrow_icon;
        }

        public PrintView getIcon() {
            return _icon;
        }

        public PrintView getBtnEdit() {
            return _btn_edit;
        }

        public PrintView getBtnAddArray() {
            return _btn_addArray;
        }

        public PrintView getBtnAddObject() {
            return _btn_addObject;
        }

        public PrintView getBtnAddProperty() {
            return _btn_addProperty;
        }

        public PrintView getBtnDelete() {
            return _btn_delete;
        }

        public PrintView getAcceptButton() {
            return _etb_Accept;
        }

        public PrintView getCancelButton() {
            return _etb_Cancel;
        }

        public TextView getTNodeName() {
            return _tvnode_name;
        }

        public TextView getENodeName() {
            return _etnode_name;
        }

        public TextView getTNodeValue() {
            return _tvnode_value;
        }

        public TextView getENodeValue() {
            return _etnode_value;
        }

        public View getButtons() {
            return _buttons;
        }
    }
}