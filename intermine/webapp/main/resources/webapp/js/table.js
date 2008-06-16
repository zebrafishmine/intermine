/**
 * Called when a whole column is selected/deselected
 */
function selectAll(columnIndex, columnClass, tableid) {
    $$('.selectable').each(function(item) {
       if(item.id.split("_")[1] == columnIndex) {
           item.checked = $('selectedObjects_' + columnIndex).checked;
           $w(item.className).each(function(className){
              if(className.startsWith('id_')) {
                 objectId = className;
                // Hightlight all cells for this object
                $$("td."+objectId).each(function(cell){
                    if(item.checked) {
                        cell.addClassName('highlightCell');
                    } else {
                        cell.removeClassName('highlightCell');
                    }
                });
              }
           });
       }
    });
    if($('selectedObjects_' + columnIndex).checked) {
        AjaxServices.selectAll(columnIndex, tableid);
        $('selectedIdFields').update(' All selected on all pages');
    } else {
        AjaxServices.selectAll(-1, tableid);
        $('selectedIdFields').update('');
    }
    disableOtherColumns(columnIndex);
    setToolbarAvailability(!$('selectedObjects_' + columnIndex).checked);
    if (isClear()) {
        enableAll();
    }
}

/**
 * Enables all checkboxes
 */
function enableAll() {
    $$('input.selectable').each(function(item) {
    	item.disabled = false;
    });
}


/**
 * Checks that nothing is selected
 */
function isClear() {
    var selectedIdFields = $('selectedIdFields');
    return (selectedIdFields.innerHTML.strip() == '');
}


/**
 * Disable columns with a different class
 */
function disableOtherColumns(index) {
    $$('input.selectable').each(function(input){
            if (input.id != 'selectedObjects_'  + index) {
                if (! input.hasClassName('index_' + index)) {
                    input.disabled = true;
                }
            }
        });
}

/**
 * Run when a user selects a keyfield in the results table.  internal is true
 * when called from other methods in this file (ie. not from an onclick in table.jsp)
 **/
function itemChecked(checkedRow, checkedColumn, tableid, checkbox, internal) {
    /*if (bagType == null) {
        var columnsToDisableArray = columnsToDisable[checkedColumn];
        if (columnsToDisableArray != null) {
        	columnsToDisableArray.each(function(item) {
        	  disableColumn(item);
        	});
        }
        bagType = checkedColumn;
    }*/

    /*if (!internal) {
        unselectColumnCheckbox(checkedColumn);
    }*/
    var objectId;
    var objectClass;
    $w(checkbox.className).each(function(className){
      	if(className.startsWith('id_')) {
      		objectId = className.sub('id_','');
      	}
        if(className.startsWith('class_')) {
            objectClass = className;
        }
    })

    // Update list and save selected state
    if(checkbox.checked) {
        AjaxServices.selectId(objectId,tableid,checkedColumn, {
              callback: function(selectedIds) {
                    $('selectedIdFields').update(selectedIds.join(', '));
                },
              async:false
            }
         );
    } else {
        AjaxServices.deSelectId(objectId,tableid, {
              callback: function(selectedIds) {
                $('selectedIdFields').update(selectedIds.join(', '));
              },
             async:false
         });
    }

    // Hightlight all cells for this object
    $$("td.id_"+objectId).each(function(cell){
        if(checkbox.checked) {
            cell.addClassName('highlightCell');
        } else {
            cell.removeClassName('highlightCell');
        }
    });

    $$("input.id_"+objectId).each(function(box){
        box.checked = checkbox.checked;
    });

        // Disable/enable other classes columns
    if (isClear()) {
        enableAll();
        bagType = null;
    } else {
        disableOtherColumns(checkedColumn);
    }

    var nothingSelected = $('selectedIdFields').innerHTML.strip() == '';

    setToolbarAvailability(nothingSelected);
    if (nothingSelected) {
        unselectColumnCheckbox(checkedColumn);
    }

    /*var columnsToHighlightArray = columnsToHighlight[checkedColumn]
    $$('td').each(function(item){
    	var splitter = item.id.split(',');
    	if(splitter[2] == checkedRow || (checkedRow == null && splitter[0].startsWith('cell'))) {
    		columnsToHighlightArray.each(function(item2) {
    		  if(item2 == splitter[1]) {
    		  	if(checkbox.checked) {
    		  		item.addClassName('highlightCell');
    		  		var classes = $w(item.className);
    		  		for(var i=0; i<classes.length; i++) {
    		  			var clazz = classes[i];
    		  			if(!isNaN(parseFloat(clazz))) {
    		  				AjaxServices.selectId(clazz,tableid);
    		  			}
    		  		}
    		  	} else {
    		  		item.removeClassName('highlightCell');
    		  	}
    		  }
    		})
    	}
    });*/
}

/**
 * disables all checkboxes in the given column
 **/
/*function disableColumn(index){
	$$('.selectable').each(function (item) {
		if (item.id.split('_')[1] == index) {
			item.disabled = 'true';
		}
	});
}*/

/**
 * de-selects a whole column of a given number
 **/
function unselectColumnCheckbox(column) {
	var enabled = true;
    $('selectedObjects_' + column).checked = false;
        $$('.selectable').each(function (item) {
        if ((item.id.split('_')[1] == column) && (item.checked)) {
           enabled = false;
           //setToolbarAvailability(false);
           return;
        }
    });
	setToolbarAvailability(enabled);
}

function setToolbarAvailability(status) {
	if ($('newBagName')) {
		$('newBagName').disabled = status;
	}
	if ($('saveNewBag')) {
    	$('saveNewBag').disabled = status;
	}
    if($('addToBag')){
            with($('addToBag')) {
                $('addToBag').disabled = status;
            }
    }
}

function onSaveBagEnter(formName) {
	var frm = document.forms[formName];
	frm.operationButton.value = 'saveNewBag';
}
