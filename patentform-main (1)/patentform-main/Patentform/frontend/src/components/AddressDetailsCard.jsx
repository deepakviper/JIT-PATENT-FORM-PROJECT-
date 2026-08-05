import React, { useState, useEffect, useRef } from 'react';
import { Home } from 'lucide-react';

function AddressDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [houseNo, setHouseNo] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [stateVal, setStateVal] = useState('');
  const [country, setCountry] = useState('');
  const [pincode, setPincode] = useState('');
  const [principalName, setPrincipalName] = useState('');
  const [telephone, setTelephone] = useState('');
  const [mobile, setMobile] = useState('');
  const [fax, setFax] = useState('');
  const [email, setEmail] = useState('');

  const isSelfTriggeredRef = useRef(false);

  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    const addr = user?.address || {};
    setHouseNo(addr.houseNo || '');
    setStreet(addr.street || '');
    setCity(addr.city || '');
    setStateVal(addr.state || '');
    setCountry(addr.country || '');
    setPincode(addr.pincode || '');
    setPrincipalName(addr.principalName || '');
    setTelephone(addr.telephone || '');
    setMobile(addr.mobile || '');
    setFax(addr.fax || '');
    setEmail(addr.email || '');
  }, [user]);

  const syncChanges = (updatedFields) => {
    isSelfTriggeredRef.current = true;

    const mergedAddress = {
      houseNo: updatedFields.hasOwnProperty('houseNo') ? updatedFields.houseNo : houseNo,
      street: updatedFields.hasOwnProperty('street') ? updatedFields.street : street,
      city: updatedFields.hasOwnProperty('city') ? updatedFields.city : city,
      state: updatedFields.hasOwnProperty('state') ? updatedFields.state : stateVal,
      country: updatedFields.hasOwnProperty('country') ? updatedFields.country : country,
      pincode: updatedFields.hasOwnProperty('pincode') ? updatedFields.pincode : pincode,
      principalName: updatedFields.hasOwnProperty('principalName') ? updatedFields.principalName : principalName,
      telephone: updatedFields.hasOwnProperty('telephone') ? updatedFields.telephone : telephone,
      mobile: updatedFields.hasOwnProperty('mobile') ? updatedFields.mobile : mobile,
      fax: updatedFields.hasOwnProperty('fax') ? updatedFields.fax : fax,
      email: updatedFields.hasOwnProperty('email') ? updatedFields.email : email,
    };

    const updatedUser = {
      ...user,
      address: mergedAddress
    };

    let updatedData = null;
    if (previewData) {
      updatedData = {
        ...previewData,
        applicant: {
          ...previewData.applicant,
          address: {
            ...previewData.applicant?.address,
            ...mergedAddress
          }
        }
      };
    } else {
      updatedData = {
        applicant: {
          name: user?.name || '',
          email: user?.email || '',
          address: mergedAddress
        },
        inventors: (user?.additionalMembers || []).map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      };
    }

    if (user && onUserUpdate) {
      onUserUpdate(updatedUser);
    }
    if (onChange) {
      onChange(updatedData);
    }
  };

  const handleFieldChange = (fieldName, val) => {
    if (fieldName === 'houseNo') setHouseNo(val);
    else if (fieldName === 'street') setStreet(val);
    else if (fieldName === 'city') setCity(val);
    else if (fieldName === 'state') setStateVal(val);
    else if (fieldName === 'country') setCountry(val);
    else if (fieldName === 'pincode') setPincode(val);
    else if (fieldName === 'principalName') setPrincipalName(val);
    else if (fieldName === 'telephone') setTelephone(val);
    else if (fieldName === 'mobile') setMobile(val);
    else if (fieldName === 'fax') setFax(val);
    else if (fieldName === 'email') setEmail(val);

    syncChanges({ [fieldName]: val });
  };

  const inputStyle = {
    background: '#F9FAFB',
    border: '1px solid #D1D5DB',
    color: '#1F2937',
    paddingLeft: '12px',
    paddingRight: '12px',
    height: '36px',
    fontSize: '13px',
    borderRadius: '6px',
    width: '100%'
  };

  const labelStyle = {
    color: '#4B5563',
    fontWeight: '600',
    fontSize: '12px',
    marginBottom: '4px'
  };

  return (
    <div className="card address-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', display: 'flex', flexDirection: 'column', height: '100%', minHeight: '520px', maxHeight: '680px', overflow: 'hidden' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px', flexShrink: 0 }}>
        <Home className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Address of the Applicant</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1, overflowY: 'auto', paddingRight: '4px' }}>
        
        {/* House No */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-houseNo">House No.</label>
          <input
            id="addr-houseNo"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter house number"
            value={houseNo}
            onChange={(e) => handleFieldChange('houseNo', e.target.value)}
          />
        </div>

        {/* Street */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-street">Street</label>
          <input
            id="addr-street"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter street"
            value={street}
            onChange={(e) => handleFieldChange('street', e.target.value)}
          />
        </div>

        {/* City */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-city">City</label>
          <input
            id="addr-city"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter city"
            value={city}
            onChange={(e) => handleFieldChange('city', e.target.value)}
          />
        </div>

        {/* State */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-state">State</label>
          <input
            id="addr-state"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter state"
            value={stateVal}
            onChange={(e) => handleFieldChange('state', e.target.value)}
          />
        </div>

        {/* Country */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-country">Country</label>
          <input
            id="addr-country"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter country"
            value={country}
            onChange={(e) => handleFieldChange('country', e.target.value)}
          />
        </div>

        {/* Pin Code */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-pincode">Pin Code</label>
          <input
            id="addr-pincode"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter pin code"
            value={pincode}
            onChange={(e) => handleFieldChange('pincode', e.target.value)}
          />
        </div>

        {/* Principal Name */}
        <div className="form-group" style={{ marginTop: '6px', borderTop: '1px solid #F3F4F6', paddingTop: '12px' }}>
          <label style={labelStyle} htmlFor="addr-principalName">Principal Name *</label>
          <input
            id="addr-principalName"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter principal name"
            value={principalName}
            onChange={(e) => handleFieldChange('principalName', e.target.value)}
            required
          />
        </div>

        {/* Telephone No. */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-telephone">Telephone No.</label>
          <input
            id="addr-telephone"
            type="tel"
            className="login-input"
            style={inputStyle}
            placeholder="Enter telephone number"
            value={telephone}
            onChange={(e) => handleFieldChange('telephone', e.target.value)}
          />
        </div>

        {/* Mobile No. */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-mobile">Mobile No.</label>
          <input
            id="addr-mobile"
            type="tel"
            className="login-input"
            style={inputStyle}
            placeholder="Enter mobile number"
            value={mobile}
            onChange={(e) => handleFieldChange('mobile', e.target.value)}
          />
        </div>

        {/* Fax No. */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-fax">Fax No.</label>
          <input
            id="addr-fax"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter fax number"
            value={fax}
            onChange={(e) => handleFieldChange('fax', e.target.value)}
          />
        </div>

        {/* Email ID */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-email">Email ID</label>
          <input
            id="addr-email"
            type="email"
            className="login-input"
            style={inputStyle}
            placeholder="Enter email ID"
            value={email}
            onChange={(e) => handleFieldChange('email', e.target.value)}
          />
        </div>

      </div>
    </div>
  );
}

export default AddressDetailsCard;
