package ch.systemsx.sybit.crkwebui.client.gui.renderers;

import ch.systemsx.sybit.crkwebui.client.controllers.MainController;
//import ch.systemsx.sybit.crkwebui.client.gui.EmptyLinkWithTooltip;
import ch.systemsx.sybit.crkwebui.client.gui.ResultsPanel;

import com.extjs.gxt.ui.client.data.BaseModel;
//import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;

/**
 * Renderer used to display details button which shows the residues.
 * @author srebniak_a
 *
 */
public class DetailsButtonCellRenderer extends DefaultCellRenderer 
{
	private MainController mainController;

	private boolean init;

	public DetailsButtonCellRenderer(MainController mainController) {
		this.mainController = mainController;
	}

	@Override
	public Object render(final BaseModel model, String property,
			ColumnData config, final int rowIndex, final int colIndex,
			ListStore<BaseModel> store, Grid<BaseModel> grid) 
	{
		if (!init) {
			init = true;
			grid.addListener(Events.ColumnResize,
					new Listener<GridEvent<BaseModel>>() {

						public void handleEvent(GridEvent<BaseModel> be) {
							for (int i = 0; i < be.getGrid().getStore()
									.getCount(); i++) {
								if (be.getGrid().getView()
										.getWidget(i, be.getColIndex()) != null
										&& be.getGrid().getView()
												.getWidget(i, be.getColIndex()) instanceof BoxComponent) {
									((BoxComponent) be.getGrid().getView()
											.getWidget(i, be.getColIndex()))
											.setWidth(be.getWidth() - 15);
								}
							}
						}
					});
		}

		
		Button detailsButton = new Button(MainController.CONSTANTS.results_grid_details_button(),
				new SelectionListener<ButtonEvent>() {

					@Override
					public void componentSelected(ButtonEvent ce) 
					{
						if((mainController.getMainViewPort().getCenterPanel().getDisplayPanel() != null) &&
						   (mainController.getMainViewPort().getCenterPanel().getDisplayPanel() instanceof ResultsPanel))
						{
							ResultsPanel resultsPanel = (ResultsPanel)mainController.getMainViewPort().getCenterPanel().getDisplayPanel();
							resultsPanel.getResultsGrid().getSelectionModel().select(rowIndex, false);
							mainController.getInterfaceResidues((Integer)resultsPanel.getResultsStore().getAt(rowIndex).get("id"));
						}
					}
				});

		detailsButton
				.setWidth(grid.getColumnModel().getColumnWidth(colIndex) - 15);
		detailsButton.setToolTip(MainController.CONSTANTS.results_grid_details_button_tooltip());

		return detailsButton;
		
		
//      following commented out code to replace the button by a link 		
//		EmptyLinkWithTooltip detailsLink = new EmptyLinkWithTooltip("Details",
//				"Click for details of the residues in the interface",
//				mainController,
//				0);
//		detailsLink.addListener(Events.OnClick, new Listener<BaseEvent>() {
//		
//		@Override
//		public void handleEvent(BaseEvent be) {
//
//			if((mainController.getMainViewPort().getCenterPanel().getDisplayPanel() != null) &&
//			   (mainController.getMainViewPort().getCenterPanel().getDisplayPanel() instanceof ResultsPanel))
//			{
//				ResultsPanel resultsPanel = (ResultsPanel)mainController.getMainViewPort().getCenterPanel().getDisplayPanel();
//				resultsPanel.getResultsGrid().getSelectionModel().select(rowIndex, false);
//				mainController.getInterfaceResidues((Integer)resultsPanel.getResultsStore().getAt(rowIndex).get("id"));
//			}
//		}
//		
//		});
//		detailsLink.addStyleName("eppic-action");
//		
//		return detailsLink;

	}

}
