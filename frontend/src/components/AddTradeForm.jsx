/**
 * ============================================================================
 * AddTradeForm.jsx — TICKET-I106
 * ============================================================================
 * WHAT:    React Hook Form-powered trade entry form.
 * HOW:     `useForm()` + `register()`.
 * WHY:     RHF handles all the boilerplate (controlled inputs, error state,
 *          submission lifecycle). Compare to Day-7 addTrade.js — 80% less code.
 * OBSERVE: Submitting an invalid form does NOT trigger a fetch — RHF blocks
 *          submission until validation passes.
 * ============================================================================
 *
 *  TODO(TICKET-I106):
 *    - all fields registered with proper validators
 *    - on submit calls apiService.createTrade()
 *    - success: navigate('/trades') + toast
 *    - server error: show error envelope message inline
 *
 *  HINT: For a zod schema, install @hookform/resolvers and zod, then:
 *    const schema = z.object({ ... });
 *    const { register, ... } = useForm({ resolver: zodResolver(schema) });
 * ============================================================================
 */
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { createTrade, ApiError } from '../services/apiService.js';

export default function AddTradeForm() {
    const navigate = useNavigate();
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors, isSubmitting }
    } = useForm();

    const onSubmit = async (data) => {
        try {
            await createTrade(data);
            navigate('/trades');
        } catch (e) {
            // TODO(TICKET-I106): map server-side field errors back to RHF.
            if (e instanceof ApiError && e.body?.fieldErrors) {
                Object.entries(e.body.fieldErrors).forEach(([field, msg]) =>
                    setError(field, { message: msg })
                );
            } else {
                setError('root.serverError', { message: e.message });
            }
        }
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)}>

            <label>Trade Ref
                <input {...register('tradeRef', {
                    required: 'required',
                    pattern: { value: /^TRD-\d{4}-\d{4}$/, message: 'format TRD-YYYY-####' }
                })} />
                {errors.tradeRef && <span className="field-error">{errors.tradeRef.message}</span>}
            </label>

            <label>Instrument ID
                <input type="number" {...register('instrumentId', {
                    required: 'required',
                    valueAsNumber: true,
                    min: { value: 1, message: 'must be > 0' }
                })} />
                {errors.instrumentId && <span className="field-error">{errors.instrumentId.message}</span>}
            </label>

            <label>Counterparty ID
                <input type="number" {...register('counterpartyId', {
                    required: 'required',
                    valueAsNumber: true,
                    min: { value: 1, message: 'must be > 0' }
                })} />
                {errors.counterpartyId && <span className="field-error">{errors.counterpartyId.message}</span>}
            </label>

            <label>Quantity
                <input type="number" step="0.0001" {...register('quantity', {
                    required: 'required',
                    min: { value: 0.0001, message: 'must be > 0' }
                })} />
                {errors.quantity && <span className="field-error">{errors.quantity.message}</span>}
            </label>

            <label>Price
                <input type="number" step="0.0001" {...register('price', {
                    required: 'required',
                    min: { value: 0.0001, message: 'must be > 0' }
                })} />
                {errors.price && <span className="field-error">{errors.price.message}</span>}
            </label>

            <label>Trade Date
                <input type="date" {...register('tradeDate', {
                    required: 'required',
                    validate: v => new Date(v) <= new Date() || 'cannot be in the future'
                })} />
                {errors.tradeDate && <span className="field-error">{errors.tradeDate.message}</span>}
            </label>

            {errors.root?.serverError && (
                <div className="error">Server: {errors.root.serverError.message}</div>
            )}

            <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Submitting…' : 'Submit'}
            </button>
        </form>
    );
}
