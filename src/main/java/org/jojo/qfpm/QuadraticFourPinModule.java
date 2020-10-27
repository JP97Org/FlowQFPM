package org.jojo.qfpm;

import static org.jojo.flow.model.util.DynamicObjectLoader.*;
import static org.jojo.flow.model.util.OK.ok;

import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jojo.flow.exc.ParsingException;
import org.jojo.flow.model.api.IInternalConfig;
import org.jojo.flow.model.api.IModulePin;
import org.jojo.flow.model.api.IRigidPin;
import org.jojo.flow.model.data.Fraction;
import org.jojo.flow.model.data.units.Frequency;
import org.jojo.flow.model.flowChart.GraphicalRepresentation;
import org.jojo.flow.model.flowChart.modules.DefaultPin;
import org.jojo.flow.model.flowChart.modules.ExternalConfig;
import org.jojo.flow.model.flowChart.modules.FlowModule;
import org.jojo.flow.model.flowChart.modules.InputPin;
import org.jojo.flow.model.flowChart.modules.ModuleGR;
import org.jojo.flow.model.flowChart.modules.ModulePinGR;
import org.jojo.flow.model.flowChart.modules.OutputPin;
import org.jojo.flow.model.flowChart.modules.RigidPin;
import org.jojo.flow.model.api.DOMStringUnion;
import org.jojo.flow.model.api.IDOM;
import org.jojo.flow.model.storeLoad.GraphicalRepresentationDOM;
import org.jojo.flow.model.storeLoad.ModulePinDOM;
import org.jojo.flow.model.storeLoad.PointDOM;
import org.jojo.flow.model.util.OK;

public class QuadraticFourPinModule extends FlowModule {
    private static final Point INPOS = new Point(50, 50); //TODO hier und generell ueberall korrekte gr setzen
    private IModulePin pinOut;
    private IModulePin pinIn;
    private List<IModulePin> rigidPins;
    
    public QuadraticFourPinModule(int id, ExternalConfig externalConfig) {
        super(id, externalConfig);
    }
    
    @Override
    public List<IModulePin> getAllModulePins() {
        final List<IModulePin> ret = new ArrayList<>();
        this.pinOut = this.pinOut == null ? loadPin(OutputPin.class.getName(), DefaultPin.class.getName(), this) : this.pinOut;
        this.pinIn = this.pinIn == null ? loadPin(InputPin.class.getName(), DefaultPin.class.getName(), this) : this.pinIn;
        this.pinIn.getGraphicalRepresentation().setPosition(INPOS);
        ret.add(this.pinOut);
        ret.add(this.pinIn);
        if (this.rigidPins == null) {
            this.rigidPins = new ArrayList<>();
            final IRigidPin rigidPinOne = loadRigidPin(this);
            ((ModulePinGR) rigidPinOne.getOutputPin().getGraphicalRepresentation()).setLinePoint(RIGID_ONE_POS());
            
            final IRigidPin rigidPinTwo = loadRigidPin(this);
            ((ModulePinGR) rigidPinTwo.getOutputPin().getGraphicalRepresentation()).setLinePoint(RIGID_TWO_POS());
            this.rigidPins.add(rigidPinOne.getOutputPin());
            this.rigidPins.add(rigidPinOne.getInputPin());
            this.rigidPins.add(rigidPinTwo.getOutputPin());
            this.rigidPins.add(rigidPinTwo.getInputPin());
        }
        ret.addAll(this.rigidPins);
        return ret;
    }

    @Override
    public Frequency<Fraction> getFrequency() {
        return Frequency.getFractionConstant(new Fraction(1));
    }

    @Override
    public void run() throws Exception {

    }

    @Override
    protected void setAllModulePins(final IDOM pinsDom) {
        if (isPinsDOMValid(pinsDom)) {
            if (this.rigidPins == null) {
                this.rigidPins = new ArrayList<>();
            }
            this.rigidPins.clear();
            final Map<String, DOMStringUnion> domMap = pinsDom.getDOMMap();
            int i = 0;
            for(var pinObj : domMap.values()) {
                if (pinObj.isDOM()) {
                    final IDOM pinDom = (IDOM) pinObj.getValue();
                    final IDOM pinCnDom = (IDOM)pinDom.getDOMMap().get(ModulePinDOM.NAME_CLASSNAME).getValue();
                    final String pinCn = pinCnDom.elemGet();
                    final IDOM pinCnDomImp = (IDOM)pinDom.getDOMMap().get(ModulePinDOM.NAME_CLASSNAME_IMP).getValue();
                    final String pinCnImp = pinCnDomImp.elemGet();
                    if (pinCnImp.equals(DefaultPin.class.getName())) {
                        if (pinCn.equals(OutputPin.class.getName())) {
                            this.pinOut = loadPin(pinCn, pinCnImp, this);
                            this.pinOut.restoreFromDOM(pinDom);
                        } else {
                            this.pinIn = loadPin(pinCn, pinCnImp, this);
                            this.pinIn.restoreFromDOM(pinDom);
                        }
                    } else {
                        if (this.rigidPins.isEmpty()) {
                            this.rigidPins.add(loadPin(pinCn, pinCnImp, this));
                        } else {
                            Point lastLinePoint = null;
                            final Point thisLinePoint = PointDOM.pointOf((IDOM) ((IDOM) pinDom.getDOMMap()
                                    .get(GraphicalRepresentationDOM.NAME).getValue()).getDOMMap()
                                    .get("linePoint").getValue());
                            int index = 0;
                            inner:
                            for (; index < this.rigidPins.size(); index++) {
                                lastLinePoint = ((ModulePinGR) this.rigidPins.get(index)
                                        .getGraphicalRepresentation()).getLinePoint();
                                if (thisLinePoint.equals(lastLinePoint)) {
                                    break inner;
                                }
                            }
                            index = index == this.rigidPins.size() ? 0 : index;
                            
                            if (pinCn.equals(OutputPin.class.getName())) {
                                if (thisLinePoint.equals(lastLinePoint)) {
                                    this.rigidPins.add(((RigidPin) this.rigidPins.get(index)
                                            .getModulePinImp()).getOutputPin());
                                } else {
                                    this.rigidPins.add(loadPin(pinCn, pinCnImp, this));
                                }
                            } else {
                                if (thisLinePoint.equals(lastLinePoint)) {
                                    this.rigidPins.add(((RigidPin) this.rigidPins.get(index)
                                            .getModulePinImp()).getInputPin());
                                } else {
                                    this.rigidPins.add(loadPin(pinCn, pinCnImp, this));
                                }
                            }
                        }
                        this.rigidPins.get(this.rigidPins.size() - 1).restoreFromDOM(pinDom);
                    }
                    i++;
                }
            }
            assert (this.rigidPins.size() == 4);
            assert (i == 6);
        }
    }

    @Override
    protected boolean isPinsDOMValid(final IDOM pinsDom) {
        Objects.requireNonNull(pinsDom);
        final Map<String, DOMStringUnion> domMap = pinsDom.getDOMMap();
        try {
            int i = 0;
            for(var pinObj : domMap.values()) {
                if (pinObj.isDOM()) {
                    ok(pinObj.isDOM(), OK.ERR_MSG_WRONG_CAST);
                    final IDOM pinDom = (IDOM) pinObj.getValue();
                    ok(pinDom.getDOMMap().get(ModulePinDOM.NAME_CLASSNAME).isDOM(), OK.ERR_MSG_WRONG_CAST);
                    final IDOM pinCnDom = (IDOM)pinDom.getDOMMap().get(ModulePinDOM.NAME_CLASSNAME).getValue();
                    final String pinCn = pinCnDom.elemGet();
                    ok(pinCn != null, OK.ERR_MSG_NULL);
                    ok(pinDom.getDOMMap().get(ModulePinDOM.NAME_CLASSNAME_IMP).isDOM(), OK.ERR_MSG_WRONG_CAST);
                    final IDOM pinCnDomImp = (IDOM)pinDom.getDOMMap().get(ModulePinDOM.NAME_CLASSNAME_IMP).getValue();
                    final String pinCnImp = pinCnDomImp.elemGet();
                    ok(pinCnImp != null, OK.ERR_MSG_NULL);
                    final IModulePin pin = ok(x -> loadPin(pinCn, pinCnImp, this), "");
                    ok(pin.isDOMValid(pinDom), "PIN " + OK.ERR_MSG_DOM_NOT_VALID);
                    i++;
                }
            }
            ok(i == 6, "to many or not enough pins should= 6, is = " + i);
            return true;
        } catch (ParsingException e) {
            e.getWarning().setAffectedElement(this).reportWarning();
            return false;
        }
    }

    @Override
    public IInternalConfig getInternalConfig() {
        return null;
    }
    
    @Override
    public void setInternalConfig(IDOM internalConfigIDOM) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean isInternalConfigDOMValid(IDOM internalConfigIDOM) {
        // TODO Auto-generated method stub
        return true;
    } 

    @Override
    public GraphicalRepresentation getGraphicalRepresentation() {
        QFPModuleGR ret = new QFPModuleGR(new Point(0,0), 10, 10, "QFPM"); //TODO correct pos and size!
        ret.setModule(this);
        return ret;
    }

    @Override
    public String serializeSimulationState() {
        return null;
    }

    @Override
    public void restoreSerializedSimulationState(String simulationState) {
        
    }
    
    public void setId(final int id) {
        super.setId(id);
    }
    
    private class QFPModuleGR extends ModuleGR {
        public QFPModuleGR(Point position, int height, int width, String iconText) {
            super(position, height, width, iconText);
        }

        @Override
        public String getInfoText() {
            return "info: qfpm";
        }

        @Override
        public Window getInternalConfigWindow() {
            return null;
        }
        
        @Override
        public void setModule(final FlowModule module) {
            super.setModule(module);
        }
    }
}
