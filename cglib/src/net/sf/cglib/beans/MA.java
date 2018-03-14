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
package net.sf.cglib.beans;

import java.io.ObjectStreamException;

class MA {
    private Long id;
    private String name;
    private String privateName;
    private int intP;        
    private long longP;        
    private boolean booleanP;
    private char charP;
    private byte byteP;
    private short shortP;
    private float floatP;
    private double doubleP;
    private String stringP;
    public  String publicField;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected Object writeReplace() throws ObjectStreamException {
        return null;
    }

    /* package */ String getPrivateName() {
        return privateName;
    }

    /* package */ void setPrivateName(String name) {
        this.privateName = name;
    }

    public int getIntP() {
        return this.intP;
    }

    public void setIntP(int intP) {
        this.intP = intP;
    }

    public long getLongP() {
        return this.longP;
    }

    public void setLongP(long longP) {
        this.longP = longP;
    }

    public boolean isBooleanP() {
        return this.booleanP;
    }

    public void setBooleanP(boolean booleanP) {
        this.booleanP = booleanP;
    }

    public char getCharP() {
        return this.charP;
    }

    public void setCharP(char charP) {
        this.charP = charP;
    }

    public byte getByteP() {
        return this.byteP;
    }

    public void setByteP(byte byteP) {
        this.byteP = byteP;
    }

    public short getShortP() {
        return this.shortP;
    }

    public void setShortP(short shortP) {
        this.shortP = shortP;
    }

    public float getFloatP() {
        return this.floatP;
    }

    public void setFloatP(float floatP) {
        this.floatP = floatP;
    }

    public double getDoubleP() {
        return this.doubleP;
    }

    public void setDoubleP(double doubleP) {
        this.doubleP = doubleP;
    }

    public String getStringP() {
        return this.stringP;
    }

    public void setStringP(String stringP) {
        this.stringP = stringP;
    }
}

